package com.example.dao;

import com.example.model.Currency;
import com.example.model.ExchangeRateCalculation;
import com.example.model.ExchangeRateResponse;
import com.example.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CurrencyDAO {

    public List<Currency> getAllCurrencies() throws SQLException {
        String sql = "SELECT ID, Code, FullName, Sign FROM Currencies ORDER BY ID";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Currency> currencies = new ArrayList<>();
            while (rs.next()) {
                currencies.add(mapCurrency(rs));
            }
            return currencies;
        }
    }

    public Currency getCurrencyByCode(String code) throws SQLException {
        String sql = "SELECT ID, Code, FullName, Sign FROM Currencies WHERE UPPER(Code) = UPPER(?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapCurrency(rs) : null;
            }
        }
    }

    public Currency addCurrency(String name, String code, String sign) throws SQLException {
        String sql = "INSERT INTO Currencies (Code, FullName, Sign) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, code);
            stmt.setString(2, name);
            stmt.setString(3, sign);

            if (stmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return new Currency(generatedKeys.getInt(1), code, name, sign);
                    }
                }
            }
            return null;
        }
    }

    public List<ExchangeRateResponse> getAllExchangeRates() throws SQLException {
        String sql = """
                SELECT 
                    er.Rate,
                    bc.ID AS BaseID, bc.Code AS BaseCode, bc.FullName AS BaseName, bc.Sign AS BaseSign,
                    tc.ID AS TargetID, tc.Code AS TargetCode, tc.FullName AS TargetName, tc.Sign AS TargetSign
                FROM ExchangeRates er
                JOIN Currencies bc ON er.BaseCurrencyId = bc.ID
                JOIN Currencies tc ON er.TargetCurrencyId = tc.ID
                ORDER BY bc.Code, tc.Code
                """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<ExchangeRateResponse> rates = new ArrayList<>();
            while (rs.next()) {
                rates.add(mapExchangeRate(rs));
            }
            return rates;
        }
    }

    public ExchangeRateResponse getExchangeRateByCodes(String fromCode, String toCode) throws SQLException {
        String sql = """
                SELECT 
                    er.Rate,
                    bc.ID AS BaseID, bc.Code AS BaseCode, bc.FullName AS BaseName, bc.Sign AS BaseSign,
                    tc.ID AS TargetID, tc.Code AS TargetCode, tc.FullName AS TargetName, tc.Sign AS TargetSign
                FROM ExchangeRates er
                JOIN Currencies bc ON er.BaseCurrencyId = bc.ID
                JOIN Currencies tc ON er.TargetCurrencyId = tc.ID
                WHERE UPPER(bc.Code) = UPPER(?) AND UPPER(tc.Code) = UPPER(?)
                """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fromCode);
            stmt.setString(2, toCode);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapExchangeRate(rs) : null;
            }
        }
    }

    public ExchangeRateResponse addExchangeRate(String baseCode, String targetCode, double rate) throws SQLException {
        Currency base = requireCurrency(baseCode);
        Currency target = requireCurrency(targetCode);

        if (getExchangeRateByCodes(baseCode, targetCode) != null) {
            throw new SQLException("Exchange rate from " + baseCode + " to " + targetCode + " already exists");
        }

        String sql = "INSERT INTO ExchangeRates (BaseCurrencyId, TargetCurrencyId, Rate) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, base.id());
            stmt.setInt(2, target.id());
            stmt.setDouble(3, rate);

            if (stmt.executeUpdate() > 0) {
                return new ExchangeRateResponse(base, target, rate);
            }
            return null;
        }
    }

    public ExchangeRateResponse updateExchangeRate(String baseCode, String targetCode, double newRate) throws SQLException {
        Currency base = requireCurrency(baseCode);
        Currency target = requireCurrency(targetCode);

        String sql = "UPDATE ExchangeRates SET Rate = ? WHERE BaseCurrencyId = ? AND TargetCurrencyId = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newRate);
            stmt.setInt(2, base.id());
            stmt.setInt(3, target.id());

            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Exchange rate from " + baseCode + " to " + targetCode + " not found");
            }
            return new ExchangeRateResponse(base, target, newRate);
        }
    }

    public ExchangeRateCalculation getConvertedAmount(String fromCode, String toCode, double amount) throws SQLException {
        ExchangeRateResponse direct = getExchangeRateByCodes(fromCode, toCode);
        if (direct != null) {
            return createCalculation(direct.baseCurrency(), direct.targetCurrency(), direct.rate(), amount);
        }

        ExchangeRateResponse reverse = getExchangeRateByCodes(toCode, fromCode);
        if (reverse != null) {
            double rate = 1.0 / reverse.rate();
            return createCalculation(reverse.targetCurrency(), reverse.baseCurrency(), rate, amount);
        }

        if (!"USD".equals(fromCode) && !"USD".equals(toCode)) {
            ExchangeRateResponse usdToFrom = getExchangeRateByCodes("USD", fromCode);
            ExchangeRateResponse usdToTo = getExchangeRateByCodes("USD", toCode);

            if (usdToFrom != null && usdToTo != null) {
                double rate = usdToTo.rate() / usdToFrom.rate();
                Currency fromCur = getCurrencyByCode(fromCode);
                Currency toCur = getCurrencyByCode(toCode);
                if (fromCur != null && toCur != null) {
                    return createCalculation(fromCur, toCur, rate, amount);
                }
            }
        }

        return null;
    }

    private Currency mapCurrency(ResultSet rs) throws SQLException {
        return new Currency(
                rs.getInt("ID"),
                rs.getString("Code"),
                rs.getString("FullName"),
                rs.getString("Sign")
        );
    }

    private ExchangeRateResponse mapExchangeRate(ResultSet rs) throws SQLException {
        Currency base = new Currency(
                rs.getInt("BaseID"),
                rs.getString("BaseCode"),
                rs.getString("BaseName"),
                rs.getString("BaseSign")
        );
        Currency target = new Currency(
                rs.getInt("TargetID"),
                rs.getString("TargetCode"),
                rs.getString("TargetName"),
                rs.getString("TargetSign")
        );
        return new ExchangeRateResponse(base, target, rs.getDouble("Rate"));
    }

    private Currency requireCurrency(String code) throws SQLException {
        Currency currency = getCurrencyByCode(code);
        if (currency == null) {
            throw new SQLException("Currency with code '" + code + "' not found");
        }
        return currency;
    }

    private ExchangeRateCalculation createCalculation(Currency from, Currency to, double rate, double amount) {
        return new ExchangeRateCalculation(from, to, rate, amount, rate * amount);
    }
}