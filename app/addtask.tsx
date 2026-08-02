import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StatusBar, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import DateTimePickerModal from "react-native-modal-datetime-picker";
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

const KEY = 'dbKey';

// --- TYPES ---
interface Task {
  id: string;
  title: string;
  description: string;
  completed: boolean;
  date: string;          // ISO String for Selected Date
  startTime?: string;    // ISO String for optional Start Time
  endTime?: string;      // ISO String for optional End Time
  createdDate: string;
}

export default function AddTask() {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(new Date());
  
  // Optional Times
  const [startTime, setStartTime] = useState<Date | null>(null);
  const [endTime, setEndTime] = useState<Date | null>(null);
  
  // Date/Time Modal State
  const [pickerMode, setPickerMode] = useState<'date' | 'time'>('date');
  const [activePickerTarget, setActivePickerTarget] = useState<'date' | 'startTime' | 'endTime'>('date');
  const [isDatePickerVisible, setDatePickerVisibility] = useState(false);
  
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  const showPicker = (mode: 'date' | 'time', target: 'date' | 'startTime' | 'endTime') => {
    setPickerMode(mode);
    setActivePickerTarget(target);
    setDatePickerVisibility(true);
  };

  const hideDatePicker = () => setDatePickerVisibility(false);

  const handleConfirm = (selectedDate: Date) => {
    if (activePickerTarget === 'date') {
      setDate(selectedDate);
    } else if (activePickerTarget === 'startTime') {
      setStartTime(selectedDate);
    } else if (activePickerTarget === 'endTime') {
      setEndTime(selectedDate);
    }
    hideDatePicker();
  };

  const storeData = async () => {
    if (!title.trim()) {
      Alert.alert("Error", "Please enter a title");
      return;
    }

    // Basic Validation: Ensure End Time is after Start Time if both are set
    if (startTime && endTime && endTime <= startTime) {
      Alert.alert("Invalid Time", "End time must be after start time");
      return;
    }

    try {
      const jsonData = await AsyncStorage.getItem(KEY);
      let existingData = jsonData ? JSON.parse(jsonData) : [];
      
      const newTask: Task = {
        id: Date.now().toString(),
        title: title.trim(),
        description: description.trim(),
        completed: false,
        date: date.toISOString(),
        startTime: startTime ? startTime.toISOString() : undefined,
        endTime: endTime ? endTime.toISOString() : undefined,
        createdDate: new Date().toISOString(),
      };

      const updatedData = [...existingData, newTask];
      await AsyncStorage.setItem(KEY, JSON.stringify(updatedData));
      
      router.replace('/'); 
    } catch (e) {
      console.error('Failed to save data', e);
      Alert.alert('Error', 'Failed to save task');
    }
  };

  const formatTime = (timeDate: Date | null) => {
    if (!timeDate) return 'Select Time';
    return timeDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />
      
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable style={styles.backBtn} onPress={() => router.back()}>
            <FontAwesome5 name='chevron-left' size={20} color={theme.title} />
          </Pressable>
          <Text style={[styles.headerTitle, { color: theme.title }]}>New Task</Text>
          <Pressable style={[styles.submitBtn, { backgroundColor: theme.accent }]} onPress={storeData}>
            <FontAwesome5 name="check" size={16} color="white" />
          </Pressable>
        </View>

        <View style={styles.content}>
          {/* Title Input */}
          <Text style={[styles.label, { color: theme.subtext }]}>Title</Text>
          <TextInput
            placeholder="What's on your mind?"
            value={title}
            onChangeText={setTitle}
            placeholderTextColor={theme.subtext + '80'}
            style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          />

          {/* Date Selector */}
          <Text style={[styles.label, { color: theme.subtext }]}>Date</Text>
          <Pressable 
            style={[styles.input, styles.dateRow, { backgroundColor: theme.card, borderColor: theme.border }]} 
            onPress={() => showPicker('date', 'date')}
          >
            <Text style={{ color: theme.title, fontWeight: '500' }}>
              {date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })}
            </Text>
            <Ionicons name="calendar-outline" size={20} color={theme.accent} />
          </Pressable>

          {/* Optional Start & End Times */}
          <Text style={[styles.label, { color: theme.subtext }]}>Time Range (Optional)</Text>
          <View style={styles.timeRowContainer}>
            {/* Start Time */}
            <View style={styles.flex1}>
              <Text style={[styles.subLabel, { color: theme.subtext }]}>Start Time</Text>
              <Pressable 
                style={[styles.input, styles.dateRow, { backgroundColor: theme.card, borderColor: theme.border }]} 
                onPress={() => showPicker('time', 'startTime')}
              >
                <Text style={{ color: startTime ? theme.title : theme.subtext + '80', fontWeight: '500', fontSize: 13 }}>
                  {formatTime(startTime)}
                </Text>
                {startTime ? (
                  <Pressable onPress={() => setStartTime(null)}>
                    <Ionicons name="close-circle" size={16} color={theme.subtext} />
                  </Pressable>
                ) : (
                  <Ionicons name="time-outline" size={18} color={theme.accent} />
                )}
              </Pressable>
            </View>

            {/* End Time */}
            <View style={styles.flex1}>
              <Text style={[styles.subLabel, { color: theme.subtext }]}>End Time</Text>
              <Pressable 
                style={[styles.input, styles.dateRow, { backgroundColor: theme.card, borderColor: theme.border }]} 
                onPress={() => showPicker('time', 'endTime')}
              >
                <Text style={{ color: endTime ? theme.title : theme.subtext + '80', fontWeight: '500', fontSize: 13 }}>
                  {formatTime(endTime)}
                </Text>
                {endTime ? (
                  <Pressable onPress={() => setEndTime(null)}>
                    <Ionicons name="close-circle" size={16} color={theme.subtext} />
                  </Pressable>
                ) : (
                  <Ionicons name="time-outline" size={18} color={theme.accent} />
                )}
              </Pressable>
            </View>
          </View>

          {/* Description Input */}
          <Text style={[styles.label, { color: theme.subtext }]}>Description (Optional)</Text>
          <TextInput
            multiline
            placeholder="Tap to add notes..."
            value={description}
            onChangeText={setDescription}
            textAlignVertical="top"
            placeholderTextColor={theme.subtext + '80'}
            style={[styles.input, styles.textArea, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          />
        </View>

        {/* Date / Time Picker Modal */}
        <DateTimePickerModal
          isVisible={isDatePickerVisible}
          mode={pickerMode}
          onConfirm={handleConfirm}
          onCancel={hideDatePicker}
          date={
            activePickerTarget === 'startTime'
              ? startTime || new Date()
              : activePickerTarget === 'endTime'
              ? endTime || new Date()
              : date
          }
          isDarkModeEnabled={colorScheme === 'dark'}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", paddingHorizontal: 20, paddingVertical: 15, justifyContent: "space-between", alignItems: "center" },
  headerTitle: { fontSize: 18, fontWeight: "800" },
  backBtn: { width: 40, height: 40, justifyContent: 'center' },
  submitBtn: { width: 38, height: 38, borderRadius: 19, justifyContent: 'center', alignItems: 'center', elevation: 2, shadowColor: '#000', shadowOpacity: 0.1, shadowRadius: 4, shadowOffset: { width: 0, height: 2 }},
  content: { paddingHorizontal: 20 },
  label: { fontSize: 13, fontWeight: "700", marginBottom: 8, marginTop: 18, marginLeft: 4 },
  subLabel: { fontSize: 11, fontWeight: "600", marginBottom: 4, marginLeft: 4 },
  input: { borderRadius: 15, padding: 14, fontSize: 15, fontWeight: '500', borderWidth: 1 },
  textArea: { minHeight: 100 },
  dateRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  timeRowContainer: { flexDirection: 'row', gap: 12 },
  flex1: { flex: 1 },
});