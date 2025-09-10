package co.com.pragma.solicitud.usecase.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class LoanMath {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int SCALE = 2; // centavos
    private LoanMath() {}

    public static BigDecimal monthlyInstallment(BigDecimal amount, Integer termMonths, BigDecimal annualRatePercent) {
        if (amount == null || termMonths == null || termMonths <= 0 || annualRatePercent == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }

        // i_m = (annual% / 100) / 12
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(100), MC)
                .divide(BigDecimal.valueOf(12), MC);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount
                    .divide(BigDecimal.valueOf(termMonths.longValue()), SCALE, RoundingMode.HALF_UP);
        }

        // (1 + i_m)^n
        BigDecimal onePlusI = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal factor = onePlusI.pow(termMonths, MC);

        // cuota = P * i_m * factor / (factor - 1)
        BigDecimal numerator = amount.multiply(monthlyRate, MC).multiply(factor, MC);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE, MC);

        return numerator
                .divide(denominator, SCALE, RoundingMode.HALF_UP);
    }
}
