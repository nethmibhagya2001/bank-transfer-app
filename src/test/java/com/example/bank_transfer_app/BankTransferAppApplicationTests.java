package com.example.bank_transfer_app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankTransferAppApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void dashboardRendersAsHtml() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("Bank Transfer Management System")))
				.andExpect(content().string(containsString("Deposit")))
				.andExpect(content().string(containsString("Withdraw")));
	}

	@Test
	void largeTransfersPageRenders() throws Exception {
		mockMvc.perform(get("/large-transfers").param("min", "10000"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("Large Transfers")));
	}

	@Test
	void accountHistoryShowsTotalSent() throws Exception {
		mockMvc.perform(get("/accounts/100001/history"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/html"))
				.andExpect(content().string(containsString("Total amount sent in successful transfers")));
	}
}
