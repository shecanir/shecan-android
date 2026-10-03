package ir.shecan.data.modelDto;

import java.util.List;

public class IssuesViewModel {

    private int totalCount;
    private int offset;
    private int limit;
    private List<IssuesDTO> issues;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public List<IssuesDTO> getIssues() {
        return issues;
    }

    public void setIssues(List<IssuesDTO> issues) {
        this.issues = issues;
    }

    public static class IssuesDTO {
        private int id;
        private ProjectDTO project;
        private TrackerDTO tracker;
        private StatusDTO status;
        private PriorityDTO priority;
        private AuthorDTO author;
        private String subject;
        private String description;
        private String startDate;
        private String dueDate;
        private int doneRatio;
        private boolean isPrivate;
        private String createdOn;
        private String updatedOn;
        private Object closedOn;
        private List<CustomFieldsDTO> customFields;

        public IssuesDTO(int id, ProjectDTO project, TrackerDTO tracker, StatusDTO status, PriorityDTO priority, AuthorDTO author, String subject, String description, String startDate, String dueDate, int doneRatio, boolean isPrivate, String createdOn, String updatedOn, Object closedOn, List<CustomFieldsDTO> customFields) {
            this.id = id;
            this.project = project;
            this.tracker = tracker;
            this.status = status;
            this.priority = priority;
            this.author = author;
            this.subject = subject;
            this.description = description;
            this.startDate = startDate;
            this.dueDate = dueDate;
            this.doneRatio = doneRatio;
            this.isPrivate = isPrivate;
            this.createdOn = createdOn;
            this.updatedOn = updatedOn;
            this.closedOn = closedOn;
            this.customFields = customFields;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public ProjectDTO getProject() {
            return project;
        }

        public void setProject(ProjectDTO project) {
            this.project = project;
        }

        public TrackerDTO getTracker() {
            return tracker;
        }

        public void setTracker(TrackerDTO tracker) {
            this.tracker = tracker;
        }

        public StatusDTO getStatus() {
            return status;
        }

        public void setStatus(StatusDTO status) {
            this.status = status;
        }

        public PriorityDTO getPriority() {
            return priority;
        }

        public void setPriority(PriorityDTO priority) {
            this.priority = priority;
        }

        public AuthorDTO getAuthor() {
            return author;
        }

        public void setAuthor(AuthorDTO author) {
            this.author = author;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getDueDate() {
            return dueDate;
        }

        public void setDueDate(String dueDate) {
            this.dueDate = dueDate;
        }

        public int getDoneRatio() {
            return doneRatio;
        }

        public void setDoneRatio(int doneRatio) {
            this.doneRatio = doneRatio;
        }

        public boolean isIsPrivate() {
            return isPrivate;
        }

        public void setIsPrivate(boolean isPrivate) {
            this.isPrivate = isPrivate;
        }

        public String getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(String createdOn) {
            this.createdOn = createdOn;
        }

        public String getUpdatedOn() {
            return updatedOn;
        }

        public void setUpdatedOn(String updatedOn) {
            this.updatedOn = updatedOn;
        }

        public Object getClosedOn() {
            return closedOn;
        }

        public void setClosedOn(Object closedOn) {
            this.closedOn = closedOn;
        }

        public List<CustomFieldsDTO> getCustomFields() {
            return customFields;
        }

        public void setCustomFields(List<CustomFieldsDTO> customFields) {
            this.customFields = customFields;
        }

        public static class ProjectDTO {
            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class TrackerDTO {
            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class StatusDTO {
            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class PriorityDTO {
            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class AuthorDTO {
            private int id;
            private String name;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }

        public static class CustomFieldsDTO {
            private int id;
            private String name;
            private String value;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }

        public static IssuesDTO createDefault() {
            return new IssuesDTO(
                    0,
                    new ProjectDTO(),
                    new TrackerDTO(),
                    new StatusDTO(),
                    new PriorityDTO(),
                    new AuthorDTO(),
                    "",
                    "",
                    "",
                    "",
                    0,
                    false,
                    "",
                    "",
                    null,
                    new java.util.ArrayList<>()
            );
        }

    }
}
