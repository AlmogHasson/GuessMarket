package dto;

import engine.Purchase;

public record PurchaseDTO(
        double totalPaid,
        double sharePaid,
        double commissionPaid
        )
{
        public PurchaseDTO(Purchase p) {
            this(p.totalPaid(), p.sharePaid(), p.commissionPaid());
        }
}
