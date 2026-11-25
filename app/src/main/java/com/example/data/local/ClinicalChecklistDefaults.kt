package com.example.data.local

object ClinicalChecklistDefaults {
    val defaultNoteCategories = listOf("Work", "Personal", "Ideas", "To-Do")

    fun initialChecklistItems(): List<ChecklistItemEntity> {
        val raw = listOf(
            // 1. Patient Identification
            "1" to ("1. Patient Identification" to "Name"),
            "2" to ("1. Patient Identification" to "Age"),
            "3" to ("1. Patient Identification" to "Sex"),
            "4" to ("1. Patient Identification" to "Address"),
            "5" to ("1. Patient Identification" to "Marital Status"),
            "6" to ("1. Patient Identification" to "Occupation"),
            "7" to ("1. Patient Identification" to "Date of Admission (DOA)"),

            // 2. Presenting Complaints
            "8" to ("2. Presenting Complaints" to "Primary Symptoms & Duration (arranged by onset)"),

            // 3. History of Presenting Illness
            "9" to ("3. History of Presenting Illness" to "Timeline (Baseline → First symptom → Progression → Current state)"),
            "18" to ("3. History of Presenting Illness" to "Previous Similar Episodes (Diagnosis, investigations, treatment, complications)"),

            // Pain (SOCRATES)
            "10" to ("Pain (SOCRATES)" to "Site (Exact location, depth)"),
            "11" to ("Pain (SOCRATES)" to "Onset (Sudden/gradual, initial trigger)"),
            "12" to ("Pain (SOCRATES)" to "Character (Sharp, dull, burning, crushing, tightness, colicky, throbbing, stabbing)"),
            "13" to ("Pain (SOCRATES)" to "Radiation (Spreading location)"),
            "14" to ("Pain (SOCRATES)" to "Associated Symptoms (System-specific)"),
            "15" to ("Pain (SOCRATES)" to "Timing (Constant/intermittent, frequency, nocturnal/diurnal)"),
            "16" to ("Pain (SOCRATES)" to "Exacerbating & Relieving Factors (Movement, position, meals, exercise, respiration, rest, meds)"),
            "17" to ("Pain (SOCRATES)" to "Severity (0-10 scale, functional impact)"),

            // 4. Symptom: Dyspnoea
            "19_1" to ("4. Symptom: Dyspnoea" to "Rest vs Exertional"),
            "19_2" to ("4. Symptom: Dyspnoea" to "Exercise Tolerance"),
            "19_3" to ("4. Symptom: Dyspnoea" to "Orthopnoea / Number of Pillows"),
            "19_4" to ("4. Symptom: Dyspnoea" to "Paroxysmal Nocturnal Dyspnoea (PND)"),
            "19_5" to ("4. Symptom: Dyspnoea" to "Wheezing"),
            "19_6" to ("4. Symptom: Dyspnoea" to "mMRC / NYHA Score Assessment"),

            // 4. Symptom: Cough & Sputum
            "20_1" to ("4. Symptom: Cough & Sputum" to "Dry vs Productive"),
            "20_2" to ("4. Symptom: Cough & Sputum" to "Volume & Colour"),
            "20_3" to ("4. Symptom: Cough & Sputum" to "Consistency & Odour"),
            "20_4" to ("4. Symptom: Cough & Sputum" to "Presence of Blood (Haemoptysis)"),
            "20_5" to ("4. Symptom: Cough & Sputum" to "ACE inhibitor (ACEi) Use"),

            // 4. Symptom: Fever
            "21_1" to ("4. Symptom: Fever" to "Pattern (Continuous, intermittent, remittent)"),
            "21_2" to ("4. Symptom: Fever" to "Chills & Rigors"),
            "21_3" to ("4. Symptom: Fever" to "Sweating / Night Sweats"),
            "21_4" to ("4. Symptom: Fever" to "Travel & Animal/Vector Exposure"),
            "21_5" to ("4. Symptom: Fever" to "Recent Procedures or Hospitalization History"),

            // 4. Symptom: Vomiting
            "22_1" to ("4. Symptom: Vomiting" to "Relation to Meals"),
            "22_2" to ("4. Symptom: Vomiting" to "Amount & Projectile Nature"),
            "22_3" to ("4. Symptom: Vomiting" to "Contents: Undigested Food"),
            "22_4" to ("4. Symptom: Vomiting" to "Contents: Bile Stain"),
            "22_5" to ("4. Symptom: Vomiting" to "Contents: Blood / Coffee-ground"),

            // 4. Symptom: Diarrhoea
            "23_1" to ("4. Symptom: Diarrhoea" to "Acute vs Chronic Onset"),
            "23_2" to ("4. Symptom: Diarrhoea" to "Stool Volume & Frequency"),
            "23_3" to ("4. Symptom: Diarrhoea" to "Characteristics: Watery, Bloody, or Mucoid"),
            "23_4" to ("4. Symptom: Diarrhoea" to "Tenesmus"),
            "23_5" to ("4. Symptom: Diarrhoea" to "Nocturnal Symptoms"),

            // 4. Symptom: Neuro - Weakness
            "24_1" to ("4. Symptom: Neuro - Weakness" to "Progression Rate"),
            "24_2" to ("4. Symptom: Neuro - Weakness" to "Proximal vs Distal Distribution"),
            "24_3" to ("4. Symptom: Neuro - Weakness" to "Symmetrical vs Asymmetrical"),
            "24_4" to ("4. Symptom: Neuro - Weakness" to "Cranial Nerve Involvement"),
            "24_5" to ("4. Symptom: Neuro - Weakness" to "Sphincter Involvement (Bladder/Bowel)"),

            // 4. Symptom: Neuro - Headache
            "25_1" to ("4. Symptom: Neuro - Headache" to "Site & Radiation"),
            "25_2" to ("4. Symptom: Neuro - Headache" to "Character & Severity (0-10)"),
            "25_3" to ("4. Symptom: Neuro - Headache" to "Photophobia & Phonophobia"),
            "25_4" to ("4. Symptom: Neuro - Headache" to "Neck Stiffness"),

            // Systems Review: Neurological
            "26" to ("Systems Review: Neurological" to "Headache"),
            "27" to ("Systems Review: Neurological" to "Dizziness / Vertigo"),
            "28" to ("Systems Review: Neurological" to "Syncope"),
            "29" to ("Systems Review: Neurological" to "Seizures & Loss of Consciousness"),
            "30" to ("Systems Review: Neurological" to "Weakness"),
            "31" to ("Systems Review: Neurological" to "Numbness / Paraesthesia"),
            "32" to ("Systems Review: Neurological" to "Tremor"),
            "33" to ("Systems Review: Neurological" to "Ataxia"),
            "34" to ("Systems Review: Neurological" to "Speech Disturbance"),
            "35" to ("Systems Review: Neurological" to "Visual Disturbance"),
            "36" to ("Systems Review: Neurological" to "Hearing Disturbance"),
            "37" to ("Systems Review: Neurological" to "Dysphagia"),
            "38" to ("Systems Review: Neurological" to "Facial Weakness"),
            "39" to ("Systems Review: Neurological" to "Cognitive Change"),
            "40" to ("Systems Review: Neurological" to "Behavioural / Personality Change"),
            "41" to ("Systems Review: Neurological" to "Bladder / Bowel Dysfunction"),

            // Systems Review: Cardiovascular
            "42" to ("Systems Review: Cardiovascular" to "Chest Pain"),
            "43" to ("Systems Review: Cardiovascular" to "Dyspnoea / Orthopnoea / PND"),
            "44" to ("Systems Review: Cardiovascular" to "Palpitations"),
            "45" to ("Systems Review: Cardiovascular" to "Syncope / Presyncope"),
            "46" to ("Systems Review: Cardiovascular" to "Ankle Swelling"),
            "47" to ("Systems Review: Cardiovascular" to "Claudication"),
            "48" to ("Systems Review: Cardiovascular" to "Peripheral Cyanosis"),
            "49" to ("Systems Review: Cardiovascular" to "CVD Risk Factors (HTN, Diabetes, Dyslipidaemia, Smoking, Obesity, Family History)"),

            // Systems Review: Respiratory
            "50" to ("Systems Review: Respiratory" to "Cough & Sputum"),
            "51" to ("Systems Review: Respiratory" to "Haemoptysis"),
            "52" to ("Systems Review: Respiratory" to "Dyspnoea & Wheeze"),
            "53" to ("Systems Review: Respiratory" to "Pleuritic Pain"),
            "54" to ("Systems Review: Respiratory" to "Fever, Night Sweats & Weight Loss"),
            "55" to ("Systems Review: Respiratory" to "Snoring & Daytime Somnolence"),
            "56" to ("Systems Review: Respiratory" to "Occupational Exposure & TB Contact"),

            // Systems Review: Gastrointestinal
            "57" to ("Systems Review: Gastrointestinal" to "Appetite & Weight Change"),
            "58" to ("Systems Review: Gastrointestinal" to "Dysphagia & Odynophagia"),
            "59" to ("Systems Review: Gastrointestinal" to "Heartburn / Reflux"),
            "60" to ("Systems Review: Gastrointestinal" to "Nausea & Vomiting"),
            "61" to ("Systems Review: Gastrointestinal" to "Haematemesis"),
            "62" to ("Systems Review: Gastrointestinal" to "Abdominal Pain & Distension"),
            "63" to ("Systems Review: Gastrointestinal" to "Change in Bowel Habit (Diarrhoea / Constipation)"),
            "64" to ("Systems Review: Gastrointestinal" to "Melena & PR Bleeding"),
            "65" to ("Systems Review: Gastrointestinal" to "Jaundice & Pruritus"),

            // Systems Review: Genitourinary
            "66" to ("Systems Review: Genitourinary" to "Dysuria, Frequency & Urgency"),
            "67" to ("Systems Review: Genitourinary" to "Nocturia, Hesitancy & Poor Stream"),
            "68" to ("Systems Review: Genitourinary" to "Retention & Incontinence"),
            "69" to ("Systems Review: Genitourinary" to "Haematuria"),
            "70" to ("Systems Review: Genitourinary" to "Loin Pain / Renal Colic"),
            "71" to ("Systems Review: Genitourinary" to "Recurrent UTI & Genital Symptoms"),

            // Systems Review: Musculoskeletal
            "72" to ("Systems Review: Musculoskeletal" to "Joint Pain, Swelling & Stiffness (Distribution)"),
            "73" to ("Systems Review: Musculoskeletal" to "Back Pain, Muscle Pain & Weakness"),
            "74" to ("Systems Review: Musculoskeletal" to "Restricted Movement & Trauma History"),
            "75" to ("Systems Review: Musculoskeletal" to "Raynaud Phenomenon & Skin Manifestations"),

            // Systems Review: Endocrine
            "76" to ("Systems Review: Endocrine" to "Weight Change & Intolerance (Heat/Cold)"),
            "77" to ("Systems Review: Endocrine" to "Tremor, Sweating & Palpitations"),
            "78" to ("Systems Review: Endocrine" to "Polyuria, Polydipsia & Polyphagia"),
            "79" to ("Systems Review: Endocrine" to "Neck Swelling, Hair & Skin Changes"),
            "80" to ("Systems Review: Endocrine" to "Menstrual Disturbance & Ring/Shoe Size Changes"),

            // Systems Review: Haematological
            "81" to ("Systems Review: Haematological" to "Pallor & Fatigue"),
            "82" to ("Systems Review: Haematological" to "Easy Bruising & Bleeding"),
            "83" to ("Systems Review: Haematological" to "Recurrent Infections & Lymph Node Enlargement"),
            "84" to ("Systems Review: Haematological" to "Fever, Night Sweats & Weight Loss"),
            "85" to ("Systems Review: Haematological" to "Bone Pain & History of Thrombosis (DVT/PE)"),

            // Systems Review: Eye / ENT / Skin
            "86" to ("Systems Review: Eye / ENT / Skin" to "Eyes: Visual Loss, Diplopia, Pain, Redness, Photophobia"),
            "87" to ("Systems Review: Eye / ENT / Skin" to "Ears: Hearing Loss, Tinnitus, Vertigo, Discharge, Pain"),
            "88" to ("Systems Review: Eye / ENT / Skin" to "Nose: Obstruction, Discharge, Epistaxis, Anosmia"),
            "89" to ("Systems Review: Eye / ENT / Skin" to "Throat: Sore Throat, Dysphagia, Odynophagia, Hoarseness, Ulcers"),
            "90" to ("Systems Review: Eye / ENT / Skin" to "Skin: Rash, Itching, Pigmentation, Ulcers, Photosensitivity, Hair/Nail changes, Lumps"),

            // Background & Formulation
            "91" to ("Background & Formulation" to "6. Past Medical History (Diagnosis, Date, Control, Complications)"),
            "92" to ("Background & Formulation" to "7. Past Surgical History"),
            "93" to ("Background & Formulation" to "8. Allergic History (Medicines, Food)"),
            "94" to ("Background & Formulation" to "9. Drug History & Substance Use (Alcohol, Smoking/Pack-years, Recreational)"),
            "95" to ("Background & Formulation" to "10. Family History"),
            "96" to ("Background & Formulation" to "11. Sexual History"),
            "97" to ("Background & Formulation" to "12. Menstrual and Obstetric History"),
            "98" to ("Background & Formulation" to "13. Social History (ADLs, Boarding, Caregiver, Depression, Education, Financial, Habits)"),
            "99" to ("Background & Formulation" to "14. Diet and Nutritional History"),
            "100" to ("Background & Formulation" to "15. Current Presentation & 16. Complications"),
            "101" to ("Background & Formulation" to "17. Patient ICE (Ideas, Concerns, Expectations)"),
            "102" to ("Background & Formulation" to "18. Problem List & 19. Case Summary"),
        )

        return raw.mapIndexed { index, (id, pair) ->
            ChecklistItemEntity(
                id = id,
                category = pair.first,
                title = pair.second,
                checked = false,
                sortOrder = index + 1,
            )
        }
    }
}
