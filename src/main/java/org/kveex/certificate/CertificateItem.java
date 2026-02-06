package org.kveex.certificate;

public record CertificateItem(
        String groupName,
        Course course,
        String lastName,
        String firstName,
        String middleName,
        RequestPlace requestPlace,
        String otherRequestPlaceText,
        boolean scholarshipInfo,
        String additionalInfo
) {
    public enum Course {
        FIRST {
            @Override
            public String toString() {
                return "Первый";
            }
        },
        SECOND {
            @Override
            public String toString() {
                return "Второй";
            }
        },
        THIRD {
            @Override
            public String toString() {
                return "Третий";
            }
        },
        FOURTH {
            @Override
            public String toString() {
                return "Четвёртый";
            }
        };

        public abstract String toString();
    }

    public enum RequestPlace {
            MILITARY_COMMISSARIAT {
                @Override
                public String toString() {
                    return "Военный комиссариат";
                }
            },
            PENSION_FUND {
                @Override
                public String toString() {
                    return "Пенсионный фонд РФ";
                }
            },
            SOCIAL_SERVICE {
                @Override
                public String toString() {
                    return "Социальная служба";
                }
            },
            OTHER {
                @Override
                public String toString() {
                    return "Другое...";
                }
            };

            public abstract String toString();
    }
}
