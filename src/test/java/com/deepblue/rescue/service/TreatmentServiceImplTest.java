package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

	@Mock
	private AnimalRepository animalRepository;

	@Mock
	private SpecialistRepository specialistRepository;

	@Mock
	private TreatmentRepository treatmentRepository;

	@Mock
	private TreatmentMapper mapper;

	@InjectMocks
	private TreatmentServiceImpl service;

	@Test
	void shouldRegisterTreatmentWhenRequestIsValid() {

		RescueCase rescueCase = rescueCase(
				"RES-2026-100",
				RescueStatus.IN_REHABILITATION,
				LocalDate.of(2026, 8, 20)
		);

		Animal animal = rescueCase.getAnimal();

		Specialist specialist = new Specialist(
				"SPEC-001",
				"Elena",
				"Vargas",
				"elena@deepblue.org",
				true
		);

		CreateTreatmentRequest request = new CreateTreatmentRequest(
				"AN-2026-100",
				"SPEC-001",
				LocalDateTime.of(2026, 8, 21, 9, 0),
				TreatmentType.WOUND_CARE,
				"Cleaning of left front flipper injury."
		);

		TreatmentResponse response = new TreatmentResponse(
				1L,
				"AN-2026-100",
				"SPEC-001",
				request.performedAt(),
				request.type(),
				request.description()
		);

		when(
				animalRepository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(animal)
		);

		when(
				specialistRepository
						.findByProfessionalCode("SPEC-001")
		).thenReturn(
				Optional.of(specialist)
		);

		when(
				treatmentRepository.save(any(Treatment.class))
		).thenAnswer(
				invocation -> invocation.getArgument(0)
		);

		when(
				mapper.toResponse(any(Treatment.class))
		).thenReturn(response);

		TreatmentResponse result = service.register(request);

		assertThat(result)
				.isEqualTo(response);

		verify(treatmentRepository)
				.save(any(Treatment.class));

		verify(mapper)
				.toResponse(any(Treatment.class));
	}

	@Test
	void shouldThrowWhenAnimalDoesNotExist() {

		CreateTreatmentRequest request = new CreateTreatmentRequest(
				"AN-999",
				"SPEC-001",
				LocalDateTime.of(2026, 8, 21, 9, 0),
				TreatmentType.WOUND_CARE,
				"Cleaning of left front flipper injury."
		);

		when(
				animalRepository.findByAnimalCode("AN-999")
		).thenReturn(
				Optional.empty()
		);

		assertThatThrownBy(
				() -> service.register(request)
		)
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("AN-999");

		verifyNoInteractions(specialistRepository);

		verify(treatmentRepository, never())
				.save(any());
	}

	@Test
	void shouldThrowWhenSpecialistIsInactive() {

		RescueCase rescueCase = rescueCase(
				"RES-2026-100",
				RescueStatus.IN_REHABILITATION,
				LocalDate.of(2026, 8, 20)
		);

		Animal animal = rescueCase.getAnimal();

		Specialist specialist = new Specialist(
				"SPEC-001",
				"Elena",
				"Vargas",
				"elena@deepblue.org",
				false
		);

		CreateTreatmentRequest request = new CreateTreatmentRequest(
				"AN-2026-100",
				"SPEC-001",
				LocalDateTime.of(2026, 8, 21, 9, 0),
				TreatmentType.WOUND_CARE,
				"Cleaning of left front flipper injury."
		);

		when(
				animalRepository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(animal)
		);

		when(
				specialistRepository
						.findByProfessionalCode("SPEC-001")
		).thenReturn(
				Optional.of(specialist)
		);

		assertThatThrownBy(
				() -> service.register(request)
		)
				.isInstanceOf(BusinessRuleException.class);

		verify(treatmentRepository, never())
				.save(any());
	}

	@Test
	void shouldThrowWhenRescueCaseIsReleased() {

		RescueCase rescueCase = rescueCase(
				"RES-2026-100",
				RescueStatus.RELEASED,
				LocalDate.of(2026, 8, 20)
		);

		Animal animal = rescueCase.getAnimal();

		Specialist specialist = new Specialist(
				"SPEC-001",
				"Elena",
				"Vargas",
				"elena@deepblue.org",
				true
		);

		CreateTreatmentRequest request = new CreateTreatmentRequest(
				"AN-2026-100",
				"SPEC-001",
				LocalDateTime.of(2026, 8, 21, 9, 0),
				TreatmentType.OBSERVATION,
				"Post release observation."
		);

		when(
				animalRepository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(animal)
		);

		when(
				specialistRepository
						.findByProfessionalCode("SPEC-001")
		).thenReturn(
				Optional.of(specialist)
		);

		assertThatThrownBy(
				() -> service.register(request)
		)
				.isInstanceOf(BusinessRuleException.class);

		verify(treatmentRepository, never())
				.save(any());
	}

	@Test
	void shouldThrowWhenPerformedAtIsBeforeRescueDate() {

		RescueCase rescueCase = rescueCase(
				"RES-2026-100",
				RescueStatus.IN_REHABILITATION,
				LocalDate.of(2026, 8, 20)
		);

		Animal animal = rescueCase.getAnimal();

		Specialist specialist = new Specialist(
				"SPEC-001",
				"Elena",
				"Vargas",
				"elena@deepblue.org",
				true
		);

		CreateTreatmentRequest request = new CreateTreatmentRequest(
				"AN-2026-100",
				"SPEC-001",
				LocalDateTime.of(2026, 8, 15, 9, 0),
				TreatmentType.WOUND_CARE,
				"Cleaning of left front flipper injury."
		);

		when(
				animalRepository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(animal)
		);

		when(
				specialistRepository
						.findByProfessionalCode("SPEC-001")
		).thenReturn(
				Optional.of(specialist)
		);

		assertThatThrownBy(
				() -> service.register(request)
		)
				.isInstanceOf(BusinessRuleException.class);

		verify(treatmentRepository, never())
				.save(any());
	}

	private RescueCase rescueCase(
			String caseCode,
			RescueStatus status,
			LocalDate rescueDate) {

		RescueCase rescueCase = new RescueCase(
				caseCode,
				rescueDate,
				"Bahia Concha",
				status
		);

		rescueCase.assignAnimal(
				new Animal(
						"AN-2026-100",
						"Green Sea Turtle",
						"Chelonia mydas",
						AnimalSex.FEMALE
				)
		);

		return rescueCase;
	}
}