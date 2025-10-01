/*package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.repositories.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public List<Loan> getLoans() {
        return loanRepository.findAll();
    }

    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));
    }

    public Loan createLoan(Loan loan) {
        return loanRepository.save(loan);
    }

    public Loan updateLoan(Long id, Loan loan) {
        return loanRepository.findById(id).map(existingLoan -> {
            existingLoan.setMember(loan.getMember());
            existingLoan.setAmount(loan.getAmount());
            existingLoan.setDuration(loan.getDuration());
            existingLoan.setBeginDate(loan.getBeginDate());
            existingLoan.setRepaymentAmount(loan.getRepaymentAmount());
            return loanRepository.save(existingLoan);
        }).orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
    }
}
*/


package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Loan;
import com.wbf.mutuelle.repositories.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
    }

    public List<Loan> getLoansByMemberId(Long memberId) {
        // Implémentez cette méthode si nécessaire
        return loanRepository.findAll(); // Temporaire
    }

    public List<Loan> getActiveLoans() {
        return loanRepository.findByIsRepaidFalse();
    }

    public Loan updateLoanStatus(Long id, Boolean isRepaid) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé"));
        loan.setIsRepaid(isRepaid);
        return loanRepository.save(loan);
    }
}