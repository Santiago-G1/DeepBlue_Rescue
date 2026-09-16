package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

	@Mock
	private RescueCaseRepository repository;

	@Mock
	private RescueCaseMapper mapper;

	@InjectMocks
	private RescueCaseServiceImpl service;

	@Test
	void shouldFindRescueCaseByCode() {

		RescueCase rescueCase = new RescueCase(
				"RES-001",
				LocalDate.of(2026, 8, 20),
				"Bahia Concha",
				RescueStatus.ADMITTED
		);

		RescueCaseResponse response = new RescueCaseResponse(
				1L,
				"RES-001",
				LocalDate.of(2026, 8, 20),
				"Bahia Concha",
				RescueStatus.ADMITTED,
				"DB-CAR",
				"AN-001"
		);

		when(
				repository.findByCaseCode("RES-001")
		).thenReturn(
				Optional.of(rescueCase)
		);

		when(
				mapper.toResponse(rescueCase)
		).thenReturn(response);

		RescueCaseResponse result =
				service.findByCode("RES-001");

		assertThat(result)
				.isEqualTo(response);

		verify(repository)
				.findByCaseCode("RES-001");

		verify(mapper)
				.toResponse(rescueCase);
	}

	@Test
	void shouldThrowWhenRescueCaseDoesNotExist() {

		when(
				repository.findByCaseCode("RES-999")
		).thenReturn(
				Optional.empty()
		);

		assertThatThrownBy(
				() -> service.findByCode("RES-999")
		)
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("RES-999");

		verify(mapper, never())
				.toResponse(any());
	}

	@Test
	void shouldChangeStatusWhenTransitionIsValid() {

		RescueCase rescueCase = new RescueCase(
				"RES-001",
				LocalDate.of(2026, 8, 20),
				"Bahia Concha",
				RescueStatus.ADMITTED
		);

		RescueCaseResponse response = new RescueCaseResponse(
				1L,
				"RES-001",
				LocalDate.of(2026, 8, 20),
				"Bahia Concha",
				RescueStatus.UNDER_EVALUATION,
				"DB-CAR",
				"AN-001"
		);

		when(
				repository.findByCaseCode("RES-001")
		).thenReturn(
				Optional.of(rescueCase)
		);

		when(
				repository.save(rescueCase)
		).thenReturn(rescueCase);

		when(
				mapper.toResponse(rescueCase)
		).thenReturn(response);

		RescueCaseResponse result = service.changeStatus(
				"RES-001",
				new ChangeRescueStatusRequest(
						RescueStatus.UNDER_EVALUATION
				)
		);

		assertThat(result)
				.isEqualTo(response);

		assertThat(rescueCase.getStatus())
				.isEqualTo(RescueStatus.UNDER_EVALUATION);

		verify(repository)
				.save(rescueCase);

		verify(mapper)
				.toResponse(rescueCase);
	}

	@Test
	void shouldThrowWhenTransitionIsInvalid() {

		RescueCase rescueCase = new RescueCase(
				"RES-001",
				LocalDate.of(2026, 8, 20),
				"Bahia Concha",
				RescueStatus.ADMITTED
		);

		when(
				repository.findByCaseCode("RES-001")
		).thenReturn(
				Optional.of(rescueCase)
		);

		assertThatThrownBy(
				() -> service.changeStatus(
						"RES-001",
						new ChangeRescueStatusRequest(
								RescueStatus.READY_FOR_RELEASE
						)
				)
		)
				.isInstanceOf(BusinessRuleException.class);

		assertThat(rescueCase.getStatus())
				.isEqualTo(RescueStatus.ADMITTED);

		verify(repository, never())
				.save(any());

		verify(mapper, never())
				.toResponse(any());
	}
}