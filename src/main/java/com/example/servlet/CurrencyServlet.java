package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.Currency;
import com.example.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/currencies")
public class CurrencyServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CurrencyServlet.class);

    private final CurrencyDAO currencyDAO;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CurrencyServlet() {
        this.currencyDAO = new CurrencyDAO();
    }

    CurrencyServlet(CurrencyDAO currencyDAO) {
        this.currencyDAO = currencyDAO;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Currency> currencies = currencyDAO.getAllCurrencies();
            String json = objectMapper.writeValueAsString(currencies);

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);
            log.info("Currency presents: {}", currencies);

        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String name = req.getParameter("name");
        String code = req.getParameter("code");
        String sign = req.getParameter("sign");

        if (name == null || name.trim().isEmpty() ||
                code == null || code.trim().isEmpty() ||
                sign == null || sign.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Отсутствует нужное поле формы")));
            return;
        }

        try {
            Currency existing = currencyDAO.getCurrencyByCode(code);
            if (existing != null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Валюта с таким кодом уже существует")));
                return;
            }

            Currency newCurrency = currencyDAO.addCurrency(name, code, sign);

            if (newCurrency != null) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write(objectMapper.writeValueAsString(newCurrency));
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"message\":\"Ошибка создания валюты\"}");
            }
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("Ошибка сервера: база данных недоступна")));
        }
    }
}