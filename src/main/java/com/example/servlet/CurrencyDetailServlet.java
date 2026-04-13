package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.Currency;
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

@WebServlet("/currency/*")
public class CurrencyDetailServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(CurrencyDetailServlet.class);

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Currency code required\"}");
            return;
        }

        String code = pathInfo.substring(1);

        try {
            Currency currency = currencyDAO.getCurrencyByCode(code);

            if (currency != null) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(objectMapper.writeValueAsString(currency));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                log.info("Currency not found: {}", code);
                resp.getWriter().write("{\"error\":\"Currency not found\"}");
            }
        } catch (SQLException e) {
            log.error("Database error while fetching currency {}", code, e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
        }
    }
}