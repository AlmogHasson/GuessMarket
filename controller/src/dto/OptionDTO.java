package dto;
import engine.Option;

public record OptionDTO (
    String option
) {
    // Constructor to create dto.OptionDTO from Option
    public OptionDTO(Option option) {
        this(option.getOptionName());
    }

    //implement all getters for the fields
    public String getOption() {
        return option;
    }
}
