package com.asusoftware.Employee_Management_API.model.dto;

import com.asusoftware.Employee_Management_API.model.LeaveStatus;

import java.util.UUID;

public record DecideLeaveRequest(
        LeaveStatus status,
        UUID approverId,
        String approverNotes
) {}
