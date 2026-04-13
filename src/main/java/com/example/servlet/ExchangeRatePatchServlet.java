package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.ExchangeRateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/exchangeRates/patch/*")
public class ExchangeRatePatchServlet extends HttpServlet {

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/[A-Z]{3}/[A-Z]{3}")) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Invalid URL\"}");
            return;
        }

        String[] parts = pathInfo.substring(1).split("/");
        String base = parts[0];
        String target = parts[1];

        String rateStr = req.getParameter("rate");
        if (rateStr == null || rateStr.trim().isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"rate required\"}");
            return;
        }

        try {
            double rate = Double.parseDouble(rateStr);
            ExchangeRateResponse updated = currencyDAO.updateExchangeRate(base, target, rate);
            resp.setStatus(200);
            resp.getWriter().write(objectMapper.writeValueAsString(updated));
        } catch (SQLException e) {
            resp.setStatus(404);
            resp.getWriter().write("{\"error\":\"Not found\"}");
        } catch (NumberFormatException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"Invalid rate\"}");
        }
    }
}