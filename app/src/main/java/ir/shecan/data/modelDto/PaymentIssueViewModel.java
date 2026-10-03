package ir.shecan.data.modelDto;

public class PaymentIssueViewModel {
    private IssuesViewModel.IssuesDTO issue;

    public IssuesViewModel.IssuesDTO getIssue() {
        return issue;
    }

    public void setIssue(IssuesViewModel.IssuesDTO issue) {
        this.issue = issue;
    }
}
