package dto;

import engine.Purchase;

public record PurchaseDTO(
        float totalPaid,
        float sharePaid,
        float commissionPaid
        )
{
        public PurchaseDTO(Purchase p) {
            this(p.getTotalPaid(), p.getSharePaid(), p.getCommissionPaid());
        }
}
