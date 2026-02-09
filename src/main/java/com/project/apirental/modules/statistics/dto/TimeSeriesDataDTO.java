package com.project.apirental.modules.statistics.dto;

import java.util.List;

// Pour les graphiques linéaires ou barres (ex: Revenus par mois)
public record TimeSeriesDataDTO(
    List<String> labels, // ex: ["Jan", "Feb", "Mar"] ou ["01/01", "02/01"]
    List<Double> values  // ex: [150000, 230000, 120000]
) {}
