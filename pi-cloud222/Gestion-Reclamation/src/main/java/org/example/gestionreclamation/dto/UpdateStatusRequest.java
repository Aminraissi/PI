package org.example.gestionreclamation.dto;

import org.example.gestionreclamation.enums.ReclamationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ReclamationStatus status) {}
