package edu.iiitd.dbms.domain;

import java.util.*;
import java.time.*;

public class Investor {
    private final int investorId;
    private final String investorName;
    private final String investorEmail;
    private final LocalDate registrationDate;

    //constructor
    public Investor(int investorId, String investorName, String investorEmail, LocalDate registrationDate){
        if(investorId < 0){
            throw new IllegalArgumentException("Investor ID cannot be negative");
        }
        if(investorName == null || investorName.isBlank()){
            throw new IllegalArgumentException("Name of the investor should exist");
        }
        if(investorEmail == null || investorEmail.isBlank()){
            throw new IllegalArgumentException("Email of the investor should exist");
        }
        this.investorId = investorId;
        this.investorName = investorName;
        this.investorEmail = investorEmail;
        this.registrationDate = registrationDate;
    }

    //getters
    public int getInvestorId(){
        return investorId;
    }
    public String getInvestorName(){
        return investorName;
    }
    public String getInvestorEmail(){
        return investorEmail;
    }
    public LocalDate getRegistrationDate(){
        return registrationDate;
    }
}
