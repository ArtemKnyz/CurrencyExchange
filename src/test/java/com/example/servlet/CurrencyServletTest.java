package com.example.servlet;

import com.example.dao.CurrencyDAO;
import com.example.model.Currency;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServletTest {

    @Mock
    private CurrencyDAO currencyDAO;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse resp;

    @InjectMocks
    private CurrencyServlet servlet;

    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        responseBody = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(responseBody));
    }

     // ===== GET =====

    @Test
    void doGet_shouldReturn200WithCurrencies() throws Exception {
        List<Currency> currencies = List.of(
                new Currency(1, "USD", "US Dollar", "$"),
                new Currency(2, "EUR", "Euro", "€")
        );
        when(currencyDAO.getAllCurrencies()).thenReturn(currencies);

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_OK);
        assertTrue(responseBody.toString().contains("USD"));
        assertTrue(responseBody.toString().contains("EUR"));
    }

    @Test
    void doGet_shouldReturn500OnDatabaseError() throws Exception {
        when(currencyDAO.getAllCurrencies()).thenThrow(new SQLException("DB down"));

        servlet.doGet(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseBody.toString().contains("Database error"));
    }

    // ===== POST =====

    @Test
    void doPost_shouldReturn201WhenCurrencyCreated() throws Exception {
        when(req.getParameter("name")).thenReturn("US Dollar");
        when(req.getParameter("code")).thenReturn("USD");
        when(req.getParameter("sign")).thenReturn("$");
        when(currencyDAO.getCurrencyByCode("USD")).thenReturn(null);
        when(currencyDAO.addCurrency("US Dollar", "USD", "$"))
                .thenReturn(new Currency(1, "USD", "US Dollar", "$"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CREATED);
        assertTrue(responseBody.toString().contains("USD"));
    }

    @Test
    void doPost_shouldReturn400WhenFieldsMissing() throws Exception {
        when(req.getParameter("name")).thenReturn("");
        when(req.getParameter("code")).thenReturn("USD");
        when(req.getParameter("sign")).thenReturn("$");

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseBody.toString().contains("Отсутствует нужное поле формы"));
    }

    @Test
    void doPost_shouldReturn409WhenCurrencyAlreadyExists() throws Exception {
        when(req.getParameter("name")).thenReturn("US Dollar");
        when(req.getParameter("code")).thenReturn("USD");
        when(req.getParameter("sign")).thenReturn("$");
        when(currencyDAO.getCurrencyByCode("USD"))
                .thenReturn(new Currency(1, "USD", "US Dollar", "$"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_CONFLICT);
        assertTrue(responseBody.toString().contains("уже существует"));
    }

    @Test
    void doPost_shouldReturn500OnDatabaseError() throws Exception {
        when(req.getParameter("name")).thenReturn("US Dollar");
        when(req.getParameter("code")).thenReturn("USD");
        when(req.getParameter("sign")).thenReturn("$");
        when(currencyDAO.getCurrencyByCode("USD")).thenThrow(new SQLException("DB down"));

        servlet.doPost(req, resp);

        verify(resp).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertTrue(responseBody.toString().contains("база данных недоступна"));
    }
}