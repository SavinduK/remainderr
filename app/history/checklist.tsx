import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useState } from 'react';
import {
    FlatList,
    Modal,
    Pressable,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    useColorScheme,
    View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from '../footer';
import { Colors } from '../theme';

const STORAGE_KEY = 'clinical_checklist_items_v2';

export interface MedicalCheckItem {
    id: string;
    category: string;
    title: string;
    checked: boolean;
}

interface SectionData {
    title: string;
    data: MedicalCheckItem[];
}

const INITIAL_CLINICAL_ITEMS: MedicalCheckItem[] = [
    // 1. Patient Identification
    { id: '1', category: '1. Patient Identification', title: 'Name', checked: false },
    { id: '2', category: '1. Patient Identification', title: 'Age', checked: false },
    { id: '3', category: '1. Patient Identification', title: 'Sex', checked: false },
    { id: '4', category: '1. Patient Identification', title: 'Address', checked: false },
    { id: '5', category: '1. Patient Identification', title: 'Marital Status', checked: false },
    { id: '6', category: '1. Patient Identification', title: 'Occupation', checked: false },
    { id: '7', category: '1. Patient Identification', title: 'Date of Admission (DOA)', checked: false },

    // 2. Presenting Complaints
    { id: '8', category: '2. Presenting Complaints', title: 'Primary Symptoms & Duration (arranged by onset)', checked: false },

    // 3. History of Presenting Illness (HPI)
    { id: '9', category: '3. History of Presenting Illness', title: 'Timeline (Baseline → First symptom → Progression → Current state)', checked: false },
    { id: '18', category: '3. History of Presenting Illness', title: 'Previous Similar Episodes (Diagnosis, investigations, treatment, complications)', checked: false },

    // 3a. History of Presenting Illness - Pain (SOCRATES)
    { id: '10', category: 'Pain (SOCRATES)', title: 'Site (Exact location, depth)', checked: false },
    { id: '11', category: 'Pain (SOCRATES)', title: 'Onset (Sudden/gradual, initial trigger)', checked: false },
    { id: '12', category: 'Pain (SOCRATES)', title: 'Character (Sharp, dull, burning, crushing, tightness, colicky, throbbing, stabbing)', checked: false },
    { id: '13', category: 'Pain (SOCRATES)', title: 'Radiation (Spreading location)', checked: false },
    { id: '14', category: 'Pain (SOCRATES)', title: 'Associated Symptoms (System-specific)', checked: false },
    { id: '15', category: 'Pain (SOCRATES)', title: 'Timing (Constant/intermittent, frequency, nocturnal/diurnal)', checked: false },
    { id: '16', category: 'Pain (SOCRATES)', title: 'Exacerbating & Relieving Factors (Movement, position, meals, exercise, respiration, rest, meds)', checked: false },
    { id: '17', category: 'Pain (SOCRATES)', title: 'Severity (0-10 scale, functional impact)', checked: false },

    // 4. Symptom-Specific Frameworks: Dyspnoea
    { id: '19_1', category: '4. Symptom: Dyspnoea', title: 'Rest vs Exertional', checked: false },
    { id: '19_2', category: '4. Symptom: Dyspnoea', title: 'Exercise Tolerance', checked: false },
    { id: '19_3', category: '4. Symptom: Dyspnoea', title: 'Orthopnoea / Number of Pillows', checked: false },
    { id: '19_4', category: '4. Symptom: Dyspnoea', title: 'Paroxysmal Nocturnal Dyspnoea (PND)', checked: false },
    { id: '19_5', category: '4. Symptom: Dyspnoea', title: 'Wheezing', checked: false },
    { id: '19_6', category: '4. Symptom: Dyspnoea', title: 'mMRC / NYHA Score Assessment', checked: false },

    // 4. Symptom-Specific Frameworks: Cough & Sputum
    { id: '20_1', category: '4. Symptom: Cough & Sputum', title: 'Dry vs Productive', checked: false },
    { id: '20_2', category: '4. Symptom: Cough & Sputum', title: 'Volume & Colour', checked: false },
    { id: '20_3', category: '4. Symptom: Cough & Sputum', title: 'Consistency & Odour', checked: false },
    { id: '20_4', category: '4. Symptom: Cough & Sputum', title: 'Presence of Blood (Haemoptysis)', checked: false },
    { id: '20_5', category: '4. Symptom: Cough & Sputum', title: 'ACE inhibitor (ACEi) Use', checked: false },

    // 4. Symptom-Specific Frameworks: Fever
    { id: '21_1', category: '4. Symptom: Fever', title: 'Pattern (Continuous, intermittent, remittent)', checked: false },
    { id: '21_2', category: '4. Symptom: Fever', title: 'Chills & Rigors', checked: false },
    { id: '21_3', category: '4. Symptom: Fever', title: 'Sweating / Night Sweats', checked: false },
    { id: '21_4', category: '4. Symptom: Fever', title: 'Travel & Animal/Vector Exposure', checked: false },
    { id: '21_5', category: '4. Symptom: Fever', title: 'Recent Procedures or Hospitalization History', checked: false },

    // 4. Symptom-Specific Frameworks: Vomiting
    { id: '22_1', category: '4. Symptom: Vomiting', title: 'Relation to Meals', checked: false },
    { id: '22_2', category: '4. Symptom: Vomiting', title: 'Amount & Projectile Nature', checked: false },
    { id: '22_3', category: '4. Symptom: Vomiting', title: 'Contents: Undigested Food', checked: false },
    { id: '22_4', category: '4. Symptom: Vomiting', title: 'Contents: Bile Stain', checked: false },
    { id: '22_5', category: '4. Symptom: Vomiting', title: 'Contents: Blood / Coffee-ground', checked: false },

    // 4. Symptom-Specific Frameworks: Diarrhoea
    { id: '23_1', category: '4. Symptom: Diarrhoea', title: 'Acute vs Chronic Onset', checked: false },
    { id: '23_2', category: '4. Symptom: Diarrhoea', title: 'Stool Volume & Frequency', checked: false },
    { id: '23_3', category: '4. Symptom: Diarrhoea', title: 'Characteristics: Watery, Bloody, or Mucoid', checked: false },
    { id: '23_4', category: '4. Symptom: Diarrhoea', title: 'Tenesmus', checked: false },
    { id: '23_5', category: '4. Symptom: Diarrhoea', title: 'Nocturnal Symptoms', checked: false },

    // 4. Symptom-Specific Frameworks: Neurological - Weakness
    { id: '24_1', category: '4. Symptom: Neuro - Weakness', title: 'Progression Rate', checked: false },
    { id: '24_2', category: '4. Symptom: Neuro - Weakness', title: 'Proximal vs Distal Distribution', checked: false },
    { id: '24_3', category: '4. Symptom: Neuro - Weakness', title: 'Symmetrical vs Asymmetrical', checked: false },
    { id: '24_4', category: '4. Symptom: Neuro - Weakness', title: 'Cranial Nerve Involvement', checked: false },
    { id: '24_5', category: '4. Symptom: Neuro - Weakness', title: 'Sphincter Involvement (Bladder/Bowel)', checked: false },

    // 4. Symptom-Specific Frameworks: Neurological - Headache
    { id: '25_1', category: '4. Symptom: Neuro - Headache', title: 'Site & Radiation', checked: false },
    { id: '25_2', category: '4. Symptom: Neuro - Headache', title: 'Character & Severity (0-10)', checked: false },
    { id: '25_3', category: '4. Symptom: Neuro - Headache', title: 'Photophobia & Phonophobia', checked: false },
    { id: '25_4', category: '4. Symptom: Neuro - Headache', title: 'Neck Stiffness', checked: false },

    // 5. Systems Review - Neurological
    { id: '26', category: 'Systems Review: Neurological', title: 'Headache', checked: false },
    { id: '27', category: 'Systems Review: Neurological', title: 'Dizziness / Vertigo', checked: false },
    { id: '28', category: 'Systems Review: Neurological', title: 'Syncope', checked: false },
    { id: '29', category: 'Systems Review: Neurological', title: 'Seizures & Loss of Consciousness', checked: false },
    { id: '30', category: 'Systems Review: Neurological', title: 'Weakness', checked: false },
    { id: '31', category: 'Systems Review: Neurological', title: 'Numbness / Paraesthesia', checked: false },
    { id: '32', category: 'Systems Review: Neurological', title: 'Tremor', checked: false },
    { id: '33', category: 'Systems Review: Neurological', title: 'Ataxia', checked: false },
    { id: '34', category: 'Systems Review: Neurological', title: 'Speech Disturbance', checked: false },
    { id: '35', category: 'Systems Review: Neurological', title: 'Visual Disturbance', checked: false },
    { id: '36', category: 'Systems Review: Neurological', title: 'Hearing Disturbance', checked: false },
    { id: '37', category: 'Systems Review: Neurological', title: 'Dysphagia', checked: false },
    { id: '38', category: 'Systems Review: Neurological', title: 'Facial Weakness', checked: false },
    { id: '39', category: 'Systems Review: Neurological', title: 'Cognitive Change', checked: false },
    { id: '40', category: 'Systems Review: Neurological', title: 'Behavioural / Personality Change', checked: false },
    { id: '41', category: 'Systems Review: Neurological', title: 'Bladder / Bowel Dysfunction', checked: false },

    // Systems Review - Cardiovascular
    { id: '42', category: 'Systems Review: Cardiovascular', title: 'Chest Pain', checked: false },
    { id: '43', category: 'Systems Review: Cardiovascular', title: 'Dyspnoea / Orthopnoea / PND', checked: false },
    { id: '44', category: 'Systems Review: Cardiovascular', title: 'Palpitations', checked: false },
    { id: '45', category: 'Systems Review: Cardiovascular', title: 'Syncope / Presyncope', checked: false },
    { id: '46', category: 'Systems Review: Cardiovascular', title: 'Ankle Swelling', checked: false },
    { id: '47', category: 'Systems Review: Cardiovascular', title: 'Claudication', checked: false },
    { id: '48', category: 'Systems Review: Cardiovascular', title: 'Peripheral Cyanosis', checked: false },
    { id: '49', category: 'Systems Review: Cardiovascular', title: 'CVD Risk Factors (HTN, Diabetes, Dyslipidaemia, Smoking, Obesity, Family History)', checked: false },

    // Systems Review - Respiratory
    { id: '50', category: 'Systems Review: Respiratory', title: 'Cough & Sputum', checked: false },
    { id: '51', category: 'Systems Review: Respiratory', title: 'Haemoptysis', checked: false },
    { id: '52', category: 'Systems Review: Respiratory', title: 'Dyspnoea & Wheeze', checked: false },
    { id: '53', category: 'Systems Review: Respiratory', title: 'Pleuritic Pain', checked: false },
    { id: '54', category: 'Systems Review: Respiratory', title: 'Fever, Night Sweats & Weight Loss', checked: false },
    { id: '55', category: 'Systems Review: Respiratory', title: 'Snoring & Daytime Somnolence', checked: false },
    { id: '56', category: 'Systems Review: Respiratory', title: 'Occupational Exposure & TB Contact', checked: false },

    // Systems Review - Gastrointestinal
    { id: '57', category: 'Systems Review: Gastrointestinal', title: 'Appetite & Weight Change', checked: false },
    { id: '58', category: 'Systems Review: Gastrointestinal', title: 'Dysphagia & Odynophagia', checked: false },
    { id: '59', category: 'Systems Review: Gastrointestinal', title: 'Heartburn / Reflux', checked: false },
    { id: '60', category: 'Systems Review: Gastrointestinal', title: 'Nausea & Vomiting', checked: false },
    { id: '61', category: 'Systems Review: Gastrointestinal', title: 'Haematemesis', checked: false },
    { id: '62', category: 'Systems Review: Gastrointestinal', title: 'Abdominal Pain & Distension', checked: false },
    { id: '63', category: 'Systems Review: Gastrointestinal', title: 'Change in Bowel Habit (Diarrhoea / Constipation)', checked: false },
    { id: '64', category: 'Systems Review: Gastrointestinal', title: 'Melena & PR Bleeding', checked: false },
    { id: '65', category: 'Systems Review: Gastrointestinal', title: 'Jaundice & Pruritus', checked: false },

    // Systems Review - Genitourinary
    { id: '66', category: 'Systems Review: Genitourinary', title: 'Dysuria, Frequency & Urgency', checked: false },
    { id: '67', category: 'Systems Review: Genitourinary', title: 'Nocturia, Hesitancy & Poor Stream', checked: false },
    { id: '68', category: 'Systems Review: Genitourinary', title: 'Retention & Incontinence', checked: false },
    { id: '69', category: 'Systems Review: Genitourinary', title: 'Haematuria', checked: false },
    { id: '70', category: 'Systems Review: Genitourinary', title: 'Loin Pain / Renal Colic', checked: false },
    { id: '71', category: 'Systems Review: Genitourinary', title: 'Recurrent UTI & Genital Symptoms', checked: false },

    // Systems Review - Musculoskeletal
    { id: '72', category: 'Systems Review: Musculoskeletal', title: 'Joint Pain, Swelling & Stiffness (Distribution)', checked: false },
    { id: '73', category: 'Systems Review: Musculoskeletal', title: 'Back Pain, Muscle Pain & Weakness', checked: false },
    { id: '74', category: 'Systems Review: Musculoskeletal', title: 'Restricted Movement & Trauma History', checked: false },
    { id: '75', category: 'Systems Review: Musculoskeletal', title: 'Raynaud Phenomenon & Skin Manifestations', checked: false },

    // Systems Review - Endocrine
    { id: '76', category: 'Systems Review: Endocrine', title: 'Weight Change & Intolerance (Heat/Cold)', checked: false },
    { id: '77', category: 'Systems Review: Endocrine', title: 'Tremor, Sweating & Palpitations', checked: false },
    { id: '78', category: 'Systems Review: Endocrine', title: 'Polyuria, Polydipsia & Polyphagia', checked: false },
    { id: '79', category: 'Systems Review: Endocrine', title: 'Neck Swelling, Hair & Skin Changes', checked: false },
    { id: '80', category: 'Systems Review: Endocrine', title: 'Menstrual Disturbance & Ring/Shoe Size Changes', checked: false },

    // Systems Review - Haematological
    { id: '81', category: 'Systems Review: Haematological', title: 'Pallor & Fatigue', checked: false },
    { id: '82', category: 'Systems Review: Haematological', title: 'Easy Bruising & Bleeding', checked: false },
    { id: '83', category: 'Systems Review: Haematological', title: 'Recurrent Infections & Lymph Node Enlargement', checked: false },
    { id: '84', category: 'Systems Review: Haematological', title: 'Fever, Night Sweats & Weight Loss', checked: false },
    { id: '85', category: 'Systems Review: Haematological', title: 'Bone Pain & History of Thrombosis (DVT/PE)', checked: false },

    // Systems Review - Eye / ENT & Skin
    { id: '86', category: 'Systems Review: Eye / ENT / Skin', title: 'Eyes: Visual Loss, Diplopia, Pain, Redness, Photophobia', checked: false },
    { id: '87', category: 'Systems Review: Eye / ENT / Skin', title: 'Ears: Hearing Loss, Tinnitus, Vertigo, Discharge, Pain', checked: false },
    { id: '88', category: 'Systems Review: Eye / ENT / Skin', title: 'Nose: Obstruction, Discharge, Epistaxis, Anosmia', checked: false },
    { id: '89', category: 'Systems Review: Eye / ENT / Skin', title: 'Throat: Sore Throat, Dysphagia, Odynophagia, Hoarseness, Ulcers', checked: false },
    { id: '90', category: 'Systems Review: Eye / ENT / Skin', title: 'Skin: Rash, Itching, Pigmentation, Ulcers, Photosensitivity, Hair/Nail changes, Lumps', checked: false },

    // 6-19. Background & Case Formulation
    { id: '91', category: 'Background & Formulation', title: '6. Past Medical History (Diagnosis, Date, Control, Complications)', checked: false },
    { id: '92', category: 'Background & Formulation', title: '7. Past Surgical History', checked: false },
    { id: '93', category: 'Background & Formulation', title: '8. Allergic History (Medicines, Food)', checked: false },
    { id: '94', category: 'Background & Formulation', title: '9. Drug History & Substance Use (Alcohol, Smoking/Pack-years, Recreational)', checked: false },
    { id: '95', category: 'Background & Formulation', title: '10. Family History', checked: false },
    { id: '96', category: 'Background & Formulation', title: '11. Sexual History', checked: false },
    { id: '97', category: 'Background & Formulation', title: '12. Menstrual and Obstetric History', checked: false },
    { id: '98', category: 'Background & Formulation', title: '13. Social History (ADLs, Boarding, Caregiver, Depression, Education, Financial, Habits)', checked: false },
    { id: '99', category: 'Background & Formulation', title: '14. Diet and Nutritional History', checked: false },
    { id: '100', category: 'Background & Formulation', title: '15. Current Presentation & 16. Complications', checked: false },
    { id: '101', category: 'Background & Formulation', title: '17. Patient ICE (Ideas, Concerns, Expectations)', checked: false },
    { id: '102', category: 'Background & Formulation', title: '18. Problem List & 19. Case Summary', checked: false },
];
export default function ClinicalChecklistScreen() {
    const colorScheme = useColorScheme() ?? 'light';
    const theme = Colors[colorScheme];

    const [items, setItems] = useState<MedicalCheckItem[]>([]);
    const [isEditMode, setIsEditMode] = useState(false);

    // Modal States
    const [deleteModalVisible, setDeleteModalVisible] = useState(false);
    const [addModalVisible, setAddModalVisible] = useState(false);
    const [selectedId, setSelectedId] = useState<string | null>(null);

    // New item input state
    const [newTitle, setNewTitle] = useState('');
    const [newCategory, setNewCategory] = useState('Systems Review: General');

    useFocusEffect(
        useCallback(() => {
            loadListItems();
        }, [])
    );

    const loadListItems = async () => {
        try {
            //await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_CLINICAL_ITEMS));
            const result = await AsyncStorage.getItem(STORAGE_KEY);
            if (result) {
                setItems(JSON.parse(result));
            } else {
                setItems(INITIAL_CLINICAL_ITEMS);
                await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_CLINICAL_ITEMS));
            }
        } catch (e) {
            console.error('Could not load clinical checklist.');
        }
    };

    const saveItemsToStorage = async (updatedItems: MedicalCheckItem[]) => {
        setItems(updatedItems);
        await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updatedItems));
    };

    const toggleCheck = (id: string) => {
        const updated = items.map(item => item.id === id ? { ...item, checked: !item.checked } : item);
        saveItemsToStorage(updated);
    };

    const confirmDelete = (id: string) => {
        setSelectedId(id);
        setDeleteModalVisible(true);
    };

    const deleteEntry = async () => {
        if (!selectedId) return;
        const updated = items.filter(item => item.id !== selectedId);
        await saveItemsToStorage(updated);
        setDeleteModalVisible(false);
        setSelectedId(null);
    };

    const handleAddItem = async () => {
        if (!newTitle.trim()) return;
        const newItem: MedicalCheckItem = {
            id: Date.now().toString(),
            category: newCategory.trim() || 'Custom Clinical Notes',
            title: newTitle.trim(),
            checked: false,
        };
        const updated = [newItem, ...items];
        await saveItemsToStorage(updated);
        setNewTitle('');
        setAddModalVisible(false);
    };

    // Group items by category for section rendering inside the card
    const groupedSections: SectionData[] = items.reduce((acc: SectionData[], item) => {
        const existingSection = acc.find(sec => sec.title === item.category);
        if (existingSection) {
            existingSection.data.push(item);
        } else {
            acc.push({ title: item.category, data: [item] });
        }
        return acc;
    }, []);

    return (
        <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]}>
            {/* Delete Confirmation Modal */}
            <Modal
                transparent
                visible={deleteModalVisible}
                animationType="fade"
                onRequestClose={() => setDeleteModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={[styles.modalContent, { backgroundColor: theme.card, borderColor: theme.border }]}>
                        <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Clinical Item?</Text>
                        <Text style={[styles.modalSubtext, { color: theme.subtext }]}>
                            This item will be removed permanently from your list.
                        </Text>

                        <View style={styles.modalActionRow}>
                            <TouchableOpacity
                                style={[styles.modalBtn, { backgroundColor: 'transparent' }]}
                                onPress={() => setDeleteModalVisible(false)}
                            >
                                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={[styles.modalBtn, { backgroundColor: theme.delete + '20' }]}
                                onPress={deleteEntry}
                            >
                                <Text style={{ color: theme.delete, fontWeight: '700' }}>Delete</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            {/* Add Item Modal */}
            <Modal
                transparent
                visible={addModalVisible}
                animationType="slide"
                onRequestClose={() => setAddModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={[styles.modalContent, { backgroundColor: theme.card, borderColor: theme.border }]}>
                        <Text style={[styles.modalTitle, { color: theme.title }]}>Add Clinical Item</Text>

                        <TextInput
                            style={[styles.input, { color: theme.title, borderColor: theme.border }]}
                            placeholder="Category (e.g., Systems Review: CNS)"
                            placeholderTextColor={theme.subtext}
                            value={newCategory}
                            onChangeText={setNewCategory}
                        />

                        <TextInput
                            style={[styles.input, { color: theme.title, borderColor: theme.border }]}
                            placeholder="Symptom or Requirement title"
                            placeholderTextColor={theme.subtext}
                            value={newTitle}
                            onChangeText={setNewTitle}
                        />

                        <View style={styles.modalActionRow}>
                            <TouchableOpacity
                                style={[styles.modalBtn, { backgroundColor: 'transparent' }]}
                                onPress={() => setAddModalVisible(false)}
                            >
                                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
                            </TouchableOpacity>

                            <TouchableOpacity
                                style={[styles.modalBtn, { backgroundColor: theme.accent }]}
                                onPress={handleAddItem}
                            >
                                <Text style={{ color: theme.title, fontWeight: '700' }}>Add Item</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            {/* Header */}
            <View style={styles.header}>
                <View>
                    <Text style={[styles.headerSubtitle, { color: theme.subtext }]}>CLINICAL WORKFLOW</Text>
                    <Text style={[styles.headerTitle, { color: theme.title }]}>History Checklist</Text>
                </View>
                <View style={{ flexDirection: 'row', gap: 10, alignItems: 'center' }}>
                    <Pressable
                        style={[
                            styles.editToggleBtn,
                            { backgroundColor: isEditMode ? theme.delete + '20' : theme.card, borderColor: theme.border }
                        ]}
                        onPress={() => setIsEditMode(!isEditMode)}
                    >
                        <Text style={[styles.editBtnText, { color: isEditMode ? theme.delete : theme.title }]}>
                            {isEditMode ? 'Done' : 'Edit'}
                        </Text>
                    </Pressable>

                    <Pressable style={[styles.addBtn, { backgroundColor: theme.accent }]} onPress={() => setAddModalVisible(true)}>
                        <Text style={[styles.addBtnText, { color: theme.title }]}>+</Text>
                    </Pressable>
                </View>
            </View>

            {/* Single Scrollable Outer Container */}
            <FlatList
                data={[groupedSections]}
                keyExtractor={() => 'single_card_container'}
                contentContainerStyle={styles.listContent}
                renderItem={({ item: sections }) => (
                    <View style={[styles.singleCardContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
                        {sections.map((section, sIndex) => (
                            <View key={section.title} style={styles.sectionBlock}>
                                {/* Section Header */}
                                <Text style={[styles.sectionHeadingTitle, { color: theme.accent }]}>
                                    {section.title.toUpperCase()}
                                </Text>

                                {/* Section Checkbox Items */}
                                {section.data.map((checkItem, iIndex) => {
                                    const isLastItem = sIndex === sections.length - 1 && iIndex === section.data.length - 1;

                                    return (
                                        <View key={checkItem.id}>
                                            <TouchableOpacity
                                                activeOpacity={0.7}
                                                onPress={() => (isEditMode ? confirmDelete(checkItem.id) : toggleCheck(checkItem.id))}
                                                style={styles.itemRow}
                                            >
                                                {/* Custom CSS Checkbox Square */}
                                                <View
                                                    style={[
                                                        styles.checkboxBox,
                                                        {
                                                            borderColor: checkItem.checked ? theme.accent : theme.subtext,
                                                            backgroundColor: checkItem.checked ? theme.accent : 'transparent',
                                                        },
                                                    ]}
                                                >
                                                    {checkItem.checked && <Text style={styles.checkmarkChar}>✓</Text>}
                                                </View>

                                                {/* Title Text */}
                                                <Text
                                                    style={[
                                                        styles.itemTitleText,
                                                        {
                                                            color: theme.title,
                                                            opacity: checkItem.checked ? 0.5 : 1,
                                                            textDecorationLine: checkItem.checked ? 'line-through' : 'none',
                                                        },
                                                    ]}
                                                >
                                                    {checkItem.title}
                                                </Text>

                                                {/* Edit Mode Remove Label */}
                                                {isEditMode && (
                                                    <TouchableOpacity onPress={() => confirmDelete(checkItem.id)}>
                                                        <Text style={[styles.removeText, { color: theme.delete }]}>Remove</Text>
                                                    </TouchableOpacity>
                                                )}
                                            </TouchableOpacity>

                                            {!isLastItem && <View style={[styles.itemDivider, { backgroundColor: theme.border + '40' }]} />}
                                        </View>
                                    );
                                })}
                            </View>
                        ))}
                    </View>
                )}
            />
            <Footer />
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: { flex: 1 },
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 20,
        paddingTop: 15,
        paddingBottom: 20,
    },
    headerSubtitle: { fontSize: 11, fontWeight: '700', letterSpacing: 1.2 },
    headerTitle: { fontSize: 26, fontWeight: '800', letterSpacing: -0.5, marginTop: 2 },
    addBtn: { width: 40, height: 40, borderRadius: 20, justifyContent: 'center', alignItems: 'center' },
    addBtnText: { fontSize: 22, fontWeight: '600', lineHeight: 24 },
    editToggleBtn: {
        paddingHorizontal: 14,
        height: 38,
        borderRadius: 19,
        borderWidth: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    editBtnText: { fontSize: 13, fontWeight: '700' },
    listContent: { paddingHorizontal: 20, paddingBottom: 40 },

    // Single Card Outer Container
    singleCardContainer: {
        borderRadius: 24,
        borderWidth: 1,
        paddingHorizontal: 18,
        paddingVertical: 14,
    },
    sectionBlock: {
        marginTop: 10,
        marginBottom: 6,
    },
    sectionHeadingTitle: {
        fontSize: 12,
        fontWeight: '800',
        letterSpacing: 1,
        marginTop: 10,
        marginBottom: 12,
    },
    itemRow: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 10,
    },
    checkboxBox: {
        width: 20,
        height: 20,
        borderRadius: 6,
        borderWidth: 1.5,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
    },
    checkmarkChar: {
        color: '#FFF',
        fontSize: 12,
        fontWeight: '900',
        marginTop: -2,
    },
    itemTitleText: {
        flex: 1,
        fontSize: 14,
        fontWeight: '600',
        lineHeight: 19,
    },
    itemDivider: {
        height: 1,
        marginLeft: 32,
    },
    removeText: {
        fontSize: 12,
        fontWeight: '700',
        marginLeft: 8,
    },

    // Modal Styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.5)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
    },
    modalContent: {
        width: '100%',
        maxWidth: 340,
        borderRadius: 24,
        padding: 24,
        alignItems: 'center',
    },
    modalTitle: {
        fontSize: 20,
        fontWeight: '800',
        marginBottom: 16,
    },
    modalSubtext: {
        fontSize: 15,
        textAlign: 'center',
        marginBottom: 24,
    },
    input: {
        width: '100%',
        height: 46,
        borderWidth: 1,
        borderRadius: 12,
        paddingHorizontal: 14,
        marginBottom: 12,
        fontSize: 14,
    },
    modalActionRow: {
        flexDirection: 'row',
        gap: 12,
        marginTop: 8,
    },
    modalBtn: {
        flex: 1,
        paddingVertical: 14,
        borderRadius: 14,
        alignItems: 'center',
        justifyContent: 'center',
    },
});