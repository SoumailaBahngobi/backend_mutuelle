package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Repayment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class ExportService {

    public byte[] exportToPdf(List<Repayment> repayments, String title) throws IOException {
        // Solution simple : retourner un CSV
        return exportToCsv(repayments);
    }

    public byte[] exportToExcel(List<Repayment> repayments, String title) throws IOException {
        // Solution simple : retourner un CSV (les utilisateurs pourront l'ouvrir avec Excel)
        return exportToCsv(repayments);
    }

    public byte[] exportToCsv(List<Repayment> repayments) throws IOException {
        StringBuilder csv = new StringBuilder();

        // En-têtes CSV
        csv.append("ID,Montant,Date Échéance,Date Paiement,N° Échéance,Total Échéances,Statut,Méthode Paiement,Référence,Prêt ID\n");

        // Données
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        for (Repayment repayment : repayments) {
            csv.append(safeToString(repayment.getId())).append(",");
            csv.append(safeToString(repayment.getAmount())).append(",");
            csv.append(repayment.getDueDate() != null ? dateFormat.format(repayment.getDueDate()) : "N/A").append(",");
            csv.append(repayment.getRepaymentDate() != null ? dateFormat.format(repayment.getRepaymentDate()) : "N/A").append(",");
            csv.append(safeToString(repayment.getInstallmentNumber())).append(",");
            csv.append(safeToString(repayment.getTotalInstallments())).append(",");
            csv.append(safeCsvString(repayment.getStatus())).append(",");
            csv.append(safeCsvString(repayment.getPaymentMethod())).append(",");
            csv.append(safeCsvString(repayment.getTransactionReference())).append(",");
            csv.append(repayment.getLoan() != null ? safeToString(repayment.getLoan().getId()) : "N/A").append("\n");
        }

        return csv.toString().getBytes("UTF-8");
    }

    // Méthode utilitaire pour gérer les valeurs nulles
    private String safeToString(Object obj) {
        if (obj == null) {
            return "N/A";
        }
        return obj.toString();
    }

    // Méthode utilitaire pour les chaînes CSV
    private String safeCsvString(String str) {
        if (str == null) {
            return "N/A";
        }
        // Échappement des guillemets et encapsulation si nécessaire
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}