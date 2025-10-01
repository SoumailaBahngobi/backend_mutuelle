/*package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.repositories.RepaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;

    public RepaymentService(RepaymentRepository repaymentRepository) {
        this.repaymentRepository = repaymentRepository;
    }

    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    public Optional<Repayment> getRepaymentById(Long id) {
        return repaymentRepository.findById(id);
    }

    public Repayment createRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    public Repayment updateRepayment(Long id, Repayment repaymentDetails) {
        Repayment repayment = repaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));
        repayment.setAmount(repaymentDetails.getAmount());
        repayment.setStatus(repaymentDetails.getStatus());
        return repaymentRepository.save(repayment);
    }

    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }
}*/

package com.wbf.mutuelle.services;
import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.repositories.RepaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;

    public RepaymentService(RepaymentRepository repaymentRepository) {
        this.repaymentRepository = repaymentRepository;
    }

    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    public Optional<Repayment> getRepaymentById(Long id) {
        return repaymentRepository.findById(id);
    }

    public Repayment createRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    public Repayment updateRepayment(Long id, Repayment repaymentDetails) {
        Repayment repayment = repaymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));
        repayment.setAmount(repaymentDetails.getAmount());
        repayment.setStatus(repaymentDetails.getStatus());
        return repaymentRepository.save(repayment);
    }

    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }
}