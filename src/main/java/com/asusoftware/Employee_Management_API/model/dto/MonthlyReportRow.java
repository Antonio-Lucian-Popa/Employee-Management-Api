package com.asusoftware.Employee_Management_API.model.dto;

import java.time.YearMonth;
import java.util.*;

public record MonthlyReportRow(String userFullName, long presentDays, long offDays) {}
