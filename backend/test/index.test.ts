import { describe, expect, it, vi } from "vitest";
import { handleRequest } from "../src/index";

function env(response: unknown = { response: { reply: "I hear the concern. What specifically have you noticed?" } }) {
  return {
    AI: { run: vi.fn().mockResolvedValue(response) },
    AI_RATE_LIMITER: { limit: vi.fn().mockResolvedValue({ success: true }) },
  };
}

describe("ReplayLead API", () => {
  it("reports health without calling AI", async () => {
    const bindings = env();
    const response = await handleRequest(new Request("https://example.test/health"), bindings);
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ ok: true, service: "replaylead-api" });
    expect(bindings.AI.run).not.toHaveBeenCalled();
  });

  it("rejects unknown scenarios", async () => {
    const response = await handleRequest(new Request("https://example.test/v1/reply", {
      method: "POST",
      body: JSON.stringify({ scenarioId: "invented", turns: [{ role: "user", text: "Hello" }] }),
      headers: { "content-type": "application/json" },
    }), env());
    expect(response.status).toBe(400);
  });

  it("returns a bounded counterpart reply", async () => {
    const response = await handleRequest(new Request("https://example.test/v1/reply", {
      method: "POST",
      body: JSON.stringify({ scenarioId: "feedback", turns: [{ role: "user", text: "Can we talk about the missed deadline?" }] }),
      headers: { "content-type": "application/json", "cf-connecting-ip": "203.0.113.7" },
    }), env());
    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({ reply: expect.any(String), model: expect.stringContaining("llama") });
  });

  it("returns 429 before calling AI", async () => {
    const bindings = env();
    bindings.AI_RATE_LIMITER.limit.mockResolvedValue({ success: false });
    const response = await handleRequest(new Request("https://example.test/v1/reply", {
      method: "POST",
      body: JSON.stringify({ scenarioId: "feedback", turns: [{ role: "user", text: "Hello" }] }),
    }), bindings);
    expect(response.status).toBe(429);
    expect(bindings.AI.run).not.toHaveBeenCalled();
  });

  it("validates and returns coaching scores", async () => {
    const bindings = env({ response: JSON.stringify({
      clarity: 82,
      empathy: 76,
      assertiveness: 70,
      actionability: 88,
      strength: "You named the behavior without labeling the person.",
      improvement: "Ask for Jordan's view before proposing the next step.",
      suggestedResponse: "How did that meeting feel from your perspective?",
    }) });
    const response = await handleRequest(new Request("https://example.test/v1/coach", {
      method: "POST",
      body: JSON.stringify({ scenarioId: "feedback", turns: [{ role: "user", text: "I noticed two deadlines slipped this month." }] }),
    }), bindings);
    expect(response.status).toBe(200);
    expect(await response.json()).toMatchObject({ clarity: 82, actionability: 88 });
  });
});
