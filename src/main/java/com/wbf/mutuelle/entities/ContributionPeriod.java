package com.wbf.mutuelle.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "contribution_period")
@Getter
@Setter
public class ContributionPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    // Montant de la cotisation individuelle pour cette période
    private BigDecimal individualAmount;

    // Dates de début et fin de la période
    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    private boolean active;

    // Constructeurs
    public ContributionPeriod() {
    }

    public Long getId() {
        return id;
    }

    public ContributionPeriod(Long id, String name, String description, BigDecimal individualAmount, Date startDate, Date endDate, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.individualAmount = individualAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getIndividualAmount() {
        return individualAmount;
    }

    public void setIndividualAmount(BigDecimal individualAmount) {
        this.individualAmount = individualAmount;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


}