package com.asusoftware.Employee_Management_API.model.dto;

import java.time.YearMonth;
import java.util.*;

public record MonthlyReportResponse(YearMonth month, List<MonthlyReportRow> rows) {}
