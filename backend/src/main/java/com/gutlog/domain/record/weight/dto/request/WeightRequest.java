package com.gutlog.domain.record.weight.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WeightRequest(
		@NotNull(message = "체중은 필수입니다.")
		@DecimalMin(value = "1.00", message = "체중은 1kg 이상이어야 합니다.")
		@DecimalMax(value = "300.00", message = "체중은 300kg 이하여야 합니다.")
		@Digits(integer = 3, fraction = 2, message = "체중은 소수점 둘째 자리까지 입력할 수 있습니다.")
		BigDecimal weight
) {
}
