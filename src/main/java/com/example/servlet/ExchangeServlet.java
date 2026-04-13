package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.ErrorResponse;
import com.example.model.ExchangeRateCalculation;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/exchange")
public class ExchangeServlet extends HttpServlet {

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String from = req.getParameter("from");
        String to = req.getParameter("to");
        String amountStr = req.getParameter("amount");

        if (from == null || from.trim().isEmpty() ||
                to == null || to.trim().isEmpty() ||
                amountStr == null || amountStr.trim().isEmpty()) {

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing required parameters: from, to, amount\"}");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Отсутствует нужное поле формы")));
            return;
        }

        try {
            ExchangeRateCalculation result = currencyDAO.getConvertedAmount(from, to, amount);

            if (result != null) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(objectMapper.writeValueAsString(result));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"Conversion path not found for " + from + " → " + to + "\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
        }
    }
}