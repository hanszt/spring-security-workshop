package com.jdriven.leaverequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;

import static com.jdriven.leaverequest.LeaveRequest.Status.APPROVED;
import static com.jdriven.leaverequest.LeaveRequest.Status.DENIED;
import static com.jdriven.leaverequest.LeaveRequest.Status.PENDING;
import static java.time.LocalDate.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class LeaveRequestControllerSpringBootWebEnvRandomPortTest {

	private static final ParameterizedTypeReference<List<LeaveRequestDTO>> TYPE_REFERENCE = new ParameterizedTypeReference<>() {};

	@Autowired
	private LeaveRequestRepository repository;

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeEach
	void beforeEach() {
		repository.clear();
	}

	@Nested
	class AuthorizeUser {

		@BeforeEach
		void beforeEach() {
			// TODO Ensure below requests are executed as user alice
		}

		@Test
		void testRequest() {
			LocalDate from = of(2019, 11, 30);
			LocalDate to = of(2019, 12, 3);
			// XXX Authenticate as Alice when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.postForEntity("/request/{employee}?from={from}&to={to}",
					null, LeaveRequestDTO.class, "Alice", from, to);
			assertThat(response.getStatusCode()).isEqualByComparingTo(ACCEPTED);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().getFromDate()).isEqualTo(from);
			assertThat(response.getBody().getToDate()).isEqualTo(to);
			assertThat(response.getBody().getStatus()).isEqualByComparingTo(PENDING);
		}

		@Test
		void testViewId() {
			LeaveRequest saved = repository.save(new LeaveRequest("Alice", of(2019, 11, 30), of(2019, 12, 3), PENDING));
			// XXX Authenticate as Alice when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.getForEntity("/view/id/{id}", LeaveRequestDTO.class, saved.getId());
			assertThat(response.getStatusCode()).isEqualByComparingTo(OK);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().getStatus()).isEqualByComparingTo(PENDING);
		}

		@Test
		void testViewEmployee() {
			repository.save(new LeaveRequest("Alice", of(2019, 11, 30), of(2019, 12, 3), PENDING));
			// XXX Authenticate as Alice when making this request
			ResponseEntity<List<LeaveRequestDTO>> response = restTemplate.exchange("/view/employee/{employee}", GET, null,
					TYPE_REFERENCE, "Alice");
			assertThat(response.getStatusCode()).isEqualByComparingTo(OK);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().get(0).getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().get(0).getStatus()).isEqualByComparingTo(PENDING);
		}
	}

	@Nested
	class AuthorizeRole {

		@BeforeEach
		void beforeEach() {
			// TODO Ensure below requests are executed as user with role HR
		}

		@Test
		void testApprove() {
			LeaveRequest saved = repository.save(new LeaveRequest("Alice", of(2019, 11, 30), of(2019, 12, 3), PENDING));
			// XXX Authenticate with HR role when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.postForEntity("/approve/{id}", null,
					LeaveRequestDTO.class, saved.getId());
			assertThat(response.getStatusCode()).isEqualByComparingTo(ACCEPTED);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().getStatus()).isEqualByComparingTo(APPROVED);
		}

		@Test
		void testApproveMissing() {
			// XXX Authenticate with HR role when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.postForEntity("/approve/{id}", null,
					LeaveRequestDTO.class, UUID.randomUUID());
			assertThat(response.getStatusCode()).isEqualByComparingTo(NO_CONTENT);
		}

		@Test
		void testDeny() {
			LeaveRequest saved = repository.save(new LeaveRequest("Alice", of(2019, 11, 30), of(2019, 12, 3), PENDING));
			// XXX Authenticate with HR role when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.postForEntity("/deny/{id}", null,
					LeaveRequestDTO.class, saved.getId());
			assertThat(response.getStatusCode()).isEqualByComparingTo(ACCEPTED);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().getStatus()).isEqualByComparingTo(DENIED);
		}

		@Test
		void testViewIdMissing() {
			// XXX Authenticate with HR role when making this request
			ResponseEntity<LeaveRequestDTO> response = restTemplate.getForEntity("/view/id/{id}", LeaveRequestDTO.class,
					UUID.randomUUID());
			assertThat(response.getStatusCode()).isEqualByComparingTo(NO_CONTENT);
		}

		@Test
		void testViewAll() {
			repository.save(new LeaveRequest("Alice", of(2019, 11, 30), of(2019, 12, 3), PENDING));
			// XXX Authenticate with HR role when making this request
			ResponseEntity<List<LeaveRequestDTO>> response = restTemplate.exchange("/view/all", GET, null, TYPE_REFERENCE);
			assertThat(response.getStatusCode()).isEqualByComparingTo(OK);
			assertThat(response.getHeaders().getContentType()).isEqualByComparingTo(APPLICATION_JSON);
			assertThat(response.getBody().get(0).getEmployee()).isEqualTo("Alice");
			assertThat(response.getBody().get(0).getStatus()).isEqualByComparingTo(PENDING);
		}
	}

}
