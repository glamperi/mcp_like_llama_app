package org.acme;

import java.util.List;

public class MaasResponse {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;

    public static class Choice {
        public int index;
        public String text;
        public String finish_reason;
        public Object logprobs;
        public String stop_reason;
        public Object prompt_logprobs;
    }

    public static class Usage {
        public int prompt_tokens;
        public int total_tokens;
        public int completion_tokens;
        public Object prompt_tokens_details;
    }
}