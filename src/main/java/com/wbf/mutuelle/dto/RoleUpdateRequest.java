package com.wbf.mutuelle.dto;

import com.wbf.mutuelle.entities.Role;
import lombok.Data;

@Data
public class RoleUpdateRequest {
    private Role role;
}