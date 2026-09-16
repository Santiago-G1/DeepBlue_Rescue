package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TreatmentServiceImpl
		implements TreatmentService {

	private final AnimalRepository animalRepository;

	private final SpecialistRepository specialistRepository;

	private final TreatmentRepository treatmentRepository;

	private final TreatmentMapper mapper;

	public TreatmentServiceImpl(
			AnimalRepository animalRepository,
			SpecialistRepository specialistRepository,
			TreatmentRepository treatmentRepository,
			TreatmentMapper mapper) {

		this.animalRepository = animalRepository;
		this.specialistRepository = specialistRepository;
		this.treatmentRepository = treatmentRepository;
		this.mapper = mapper;
	}

	@Override
	public List<TreatmentResponse> findByAnimalCode(
			String animalCode) {

		return treatmentRepository
				.findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
				.stream()
				.map(mapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public TreatmentResponse register(
			CreateTreatmentRequest request) {

		Animal animal = animalRepository
				.findByAnimalCode(request.animalCode())
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"Animal not found: "
										+ request.animalCode()
						)
				);

		Specialist specialist = specialistRepository
				.findByProfessionalCode(request.specialistCode())
				.orElseThrow(
						() -> new ResourceNotFoundException(
								"Specialist not found: "
										+ request.specialistCode()
						)
				);

		if (!specialist.isActive()) {

			throw new BusinessRuleException(
					"Cannot register treatment because the specialist is inactive: "
							+ request.specialistCode()
			);
		}

		RescueCase rescueCase = animal.getRescueCase();

		RescueStatus status = rescueCase.getStatus();

		if (status == RescueStatus.RELEASED
				|| status == RescueStatus.CLOSED) {

			throw new BusinessRuleException(
					"Cannot register treatment because the case is "
							+ status
							+ ": "
							+ rescueCase.getCaseCode()
			);
		}

		if (request.performedAt()
				.toLocalDate()
				.isBefore(rescueCase.getRescueDate())) {

			throw new BusinessRuleException(
					"Cannot register treatment because the performed date "
							+ request.performedAt()
							+ " is before the rescue date "
							+ rescueCase.getRescueDate()
			);
		}

		Treatment treatment = new Treatment(
				request.performedAt(),
				request.type(),
				request.description()
		);

		treatment.setAnimal(animal);
		treatment.setSpecialist(specialist);

		Treatment saved = treatmentRepository.save(treatment);

		return mapper.toResponse(saved);
	}
}