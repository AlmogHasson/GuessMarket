package dto;

public sealed interface OptionDTO permits LMSROptionDTO, OBOptionDTO {
    String optionName();
    double currentValue();
    int totalSharesBought();
}
