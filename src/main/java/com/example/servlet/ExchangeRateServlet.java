package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.ErrorResponse;
import com.example.model.ExchangeRateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/exchangeRates")
public class ExchangeRateServlet extends HttpServlet {

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<ExchangeRateResponse> rates = currencyDAO.getAllExchangeRates();
            String json = objectMapper.writeValueAsString(rates);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

            if (newRate != null) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(objectMapper.writeValueAsString(newRate));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"Failed to create exchange rate\"}");
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("already exists")) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"error\":\"Exchange rate from " + baseCode + " to " + targetCode + " already exists\"}");
            } else if (msg.contains("not found")) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                //resp.getWriter().write("{\"error\":\"" + msg + "\"}");
                resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Обменный курс не найден")));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"Database error\"}");
            }
        }
    }
}


