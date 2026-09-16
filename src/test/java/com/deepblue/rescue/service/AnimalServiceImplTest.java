package com.deepblue.rescue.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

	@Mock
	private AnimalRepository repository;

	@Mock
	private AnimalMapper mapper;

	@InjectMocks
	private AnimalServiceImpl service;

	@Test
	void shouldFindAnimalByCode() {

		Animal animal = animal(RescueStatus.IN_REHABILITATION);

		AnimalResponse response = new AnimalResponse(
				1L,
				"AN-2026-100",
				"Green Sea Turtle",
				"Chelonia mydas",
				AnimalSex.FEMALE,
				"RES-2026-100",
				RescueStatus.IN_REHABILITATION
		);

		when(
				repository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(animal)
		);

		when(
				mapper.toResponse(animal)
		).thenReturn(response);

		AnimalResponse result =
				service.findByCode("AN-2026-100");

		assertThat(result)
				.isEqualTo(response);

		verify(repository)
				.findByAnimalCode("AN-2026-100");

		verify(mapper)
				.toResponse(animal);
	}

	@Test
	void shouldThrowWhenAnimalDoesNotExist() {

		when(
				repository.findByAnimalCode("AN-999")
		).thenReturn(
				Optional.empty()
		);

		assertThatThrownBy(
				() -> service.findByCode("AN-999")
		)
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("AN-999");
	}

	@Test
	void shouldFindAnimalsInRehabilitation() {

		Animal animal = animal(RescueStatus.IN_REHABILITATION);

		AnimalResponse response = new AnimalResponse(
				1L,
				"AN-2026-100",
				"Green Sea Turtle",
				"Chelonia mydas",
				AnimalSex.FEMALE,
				"RES-2026-100",
				RescueStatus.IN_REHABILITATION
		);

		when(
				repository.findByRescueCaseStatus(
						RescueStatus.IN_REHABILITATION
				)
		).thenReturn(
				List.of(animal)
		);

		when(
				mapper.toResponse(animal)
		).thenReturn(response);

		List<AnimalResponse> result =
				service.findAnimalsInRehabilitation();

		assertThat(result)
				.containsExactly(response);
	}

	@Test
	void shouldAllowTreatmentWhenAnimalIsInRehabilitation() {

		when(
				repository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(
						animal(RescueStatus.IN_REHABILITATION)
				)
		);

		assertThat(
				service.canReceiveTreatment("AN-2026-100")
		).isTrue();
	}

	@Test
	void shouldAllowTreatmentWhenAnimalIsUnderEvaluation() {

		when(
				repository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(
						animal(RescueStatus.UNDER_EVALUATION)
				)
		);

		assertThat(
				service.canReceiveTreatment("AN-2026-100")
		).isTrue();
	}

	@Test
	void shouldNotAllowTreatmentWhenAnimalIsReleased() {

		when(
				repository.findByAnimalCode("AN-2026-100")
		).thenReturn(
				Optional.of(
						animal(RescueStatus.RELEASED)
				)
		);

		assertThat(
				service.canReceiveTreatment("AN-2026-100")
		).isFalse();
	}

	private Animal animal(RescueStatus status) {

		RescueCase rescueCase = new RescueCase(
				"RES-2026-100",
				LocalDate.of(2026, 8, 20),
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

		return rescueCase.getAnimal();
	}
}