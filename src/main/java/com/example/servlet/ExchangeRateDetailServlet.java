package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.ExchangeRateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(
        urlPatterns = "/exchangeRates/*",
        initParams = @WebInitParam(name = "jakarta.servlet.http.HttpServlet.service", value = "true")
)
public class ExchangeRateDetailServlet extends HttpServlet {

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetAll(req, resp);
            } else if (pathInfo.matches("/[A-Z]{3}/[A-Z]{3}")) {
                handleGetOne(req, resp, pathInfo);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Invalid URL\"}");
            }
        } catch (SQLException e) {
            throw new ServletException("Database error in GET", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            handlePost(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Database error in POST", e);
        }
    }

    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo != null && pathInfo.matches("/[A-Z]{3}/[A-Z]{3}")) {
                handlePatch(req, resp, pathInfo);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Invalid URL for PATCH. Use /exchangeRates/XXX/YYY\"}");
            }
        } catch (SQLException e) {
            throw new ServletException("Database error in PATCH", e);
        }
    }

    private void handleGetAll(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
        List<ExchangeRateResponse> rates = currencyDAO.getAllExchangeRates();
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(objectMapper.writeValueAsString(rates));
    }

    private void handleGetOne(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException, SQLException {
        String[] parts = pathInfo.substring(1).split("/");
        String fromCode = parts[0];
        String toCode = parts[1];

        ExchangeRateResponse rate = currencyDAO.getExchangeRateByCodes(fromCode, toCode);
        resp.setContentType("application/json;charset=UTF-8");

        if (rate != null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(objectMapper.writeValueAsString(rate));
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("{\"error\":\"Exchange rate not found for " + fromCode + " → " + toCode + "\"}");
        }
    }

    private void handlePost(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
        resp.setContentType("application/json;charset=UTF-8");

        String baseCode = req.getParameter("baseCurrencyCode");
        String targetCode = req.getParameter("targetCurrencyCode");
        String rateStr = req.getParameter("rate");

        if (baseCode == null || baseCode.trim().isEmpty() ||
                targetCode == null || targetCode.trim().isEmpty() ||
                rateStr == null || rateStr.trim().isEmpty()) {

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing required fields: baseCurrencyCode, targetCurrencyCode, rate\"}");
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid rate value. Must be a number\"}");
            return;
        }

        try {
            ExchangeRateResponse newRate = currencyDAO.addExchangeRate(baseCode, targetCode, rate);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write(objectMapper.writeValueAsString(newRate));
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("already exists")) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"error\":\"Exchange rate from " + baseCode + " to " + targetCode + " already exists\"}");
            } else if (msg.contains("not found")) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"" + msg + "\"}");
            } else {
                throw e;
            }
        }
    }

    private void handlePatch(HttpServletRequest req, HttpServletResponse resp, String pathInfo) throws IOException, SQLException {
        String[] parts = pathInfo.substring(1).split("/");
        String baseCode = parts[0];
        String targetCode = parts[1];

        String rateStr = req.getParameter("rate");
        if (rateStr == null || rateStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing required field: rate\"}");
            return;
        }

        double rate;
        try {
            rate = Double.parseDouble(rateStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid rate value\"}");
            return;
        }

        ExchangeRateResponse updated = currencyDAO.updateExchangeRate(baseCode, targetCode, rate);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(objectMapper.writeValueAsString(updated));
    }
}