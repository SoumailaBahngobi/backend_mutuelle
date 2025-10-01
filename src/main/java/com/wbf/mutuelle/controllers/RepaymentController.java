/*package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("repayment")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentService repaymentService;

    @GetMapping
    public List<Repayment> getAllRepayments() {
        return repaymentService.getAllRepayments();
    }

    @GetMapping("/{id}")
    public Repayment getRepaymentById(@PathVariable Long id) {
        return repaymentService.getRepaymentById(id)
                .orElseThrow(() -> new RuntimeException("Repayment not found with id " + id));
    }

    @PostMapping
    public Repayment createRepayment(@RequestBody Repayment repayment) {
        return repaymentService.createRepayment(repayment);
    }

    @PutMapping("/{id}")
    public Repayment updateRepayment(@PathVariable Long id, @RequestBody Repayment repayment) {
        return repaymentService.updateRepayment(id, repayment);
    }

    @DeleteMapping("/{id}")
    public void deleteRepayment(@PathVariable Long id) {
        repaymentService.deleteRepayment(id);
    }
}
*/

package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Repayment;
import com.wbf.mutuelle.services.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mut/repayment")
@RequiredArgsConstructor
public class RepaymentController {

    private final RepaymentService repaymentService;

    @GetMapping
    public List<Repayment> getAllRepayments() {
        return repaymentService.getAllRepayments();
    }

    @GetMapping("/{id}")
    public Repayment getRepaymentById(@PathVariable Long id) {
        return repaymentService.getRepaymentById(id)
                .orElseThrow(() -> new RuntimeException("Remboursement non trouvé"));
    }

    @PostMapping
    public Repayment createRepayment(@RequestBody Repayment repayment) {
        return repaymentService.createRepayment(repayment);
    }

    @PutMapping("/{id}")
    public Repayment updateRepayment(@PathVariable Long id, @RequestBody Repayment repayment) {
        return repaymentService.updateRepayment(id, repayment);
    }

    @DeleteMapping("/{id}")
    public void deleteRepayment(@PathVariable Long id) {
        repaymentService.deleteRepayment(id);
    }
}