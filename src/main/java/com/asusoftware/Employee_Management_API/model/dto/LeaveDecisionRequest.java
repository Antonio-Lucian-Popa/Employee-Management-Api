package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.LeaveStatus;

public record LeaveDecisionRequest(LeaveStatus status, String note) {}
