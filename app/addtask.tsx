import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StatusBar, StyleSheet, Switch, Text, TextInput, View, useColorScheme } from 'react-native';
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
  date: string; 
  createdDate: string; // Added createdDate
  type: 'daily' | 'long-term';
  recurring: boolean; 
}

export default function AddTask() {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<'daily' | 'long-term'>('daily');
  const [isRecurring, setIsRecurring] = useState(false);
  const [date, setDate] = useState(new Date());
  const [isDatePickerVisible, setDatePickerVisibility] = useState(false);
  
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  const showDatePicker = () => setDatePickerVisibility(true);
  const hideDatePicker = () => setDatePickerVisibility(false);

  const handleConfirm = (selectedDate: Date) => {
    setDate(selectedDate);
    hideDatePicker();
  };

  const updateHistoryEntry = async (task: Task, action: 'add' | 'update' | 'delete') => {
    try {
      const historyData = await AsyncStorage.getItem('historyKey');
      let history = historyData ? JSON.parse(historyData) : {};
      const dateKey = new Date(task.date).toISOString().split('T')[0];

      if (!history[dateKey]) {
        history[dateKey] = { completed: [], incomplete: [] };
      }

      const cleanLists = (title: string) => {
        history[dateKey].completed = history[dateKey].completed.filter((t: string) => t !== title);
        history[dateKey].incomplete = history[dateKey].incomplete.filter((t: string) => t !== title);
      };

      if (action === 'delete') {
        cleanLists(task.title);
      } else {
        cleanLists(task.title);
        if (task.completed) {
          history[dateKey].completed.push(task.title);
        } else {
          history[dateKey].incomplete.push(task.title);
        }
      }

      await AsyncStorage.setItem('historyKey', JSON.stringify(history));
    } catch (e) {
      console.error("History sync error:", e);
    }
  };

  const storeData = async () => {
    if (!title.trim()) {
      Alert.alert("Error", "Please enter a title");
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
        date: type === 'daily' ? new Date().toISOString() : date.toISOString(),
        createdDate: new Date().toISOString(), // Fixed: Saves creation date timestamp
        type: type,
        recurring: type === 'daily' ? isRecurring : false,
      };

      const updatedData = [...existingData, newTask];
      await AsyncStorage.setItem(KEY, JSON.stringify(updatedData));

      if (newTask.type === 'daily') {
        await updateHistoryEntry(newTask, 'add');
      }
      
      router.replace('/'); 
    } catch (e) {
      console.error('Failed to save data', e);
      Alert.alert('Error', 'Failed to save task');
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />
      
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable style={styles.backBtn} onPress={() => router.back()}>
            <FontAwesome5 name='chevron-left' size={22} color={theme.title} />
          </Pressable>
          <Text style={[styles.headerTitle, { color: theme.title }]}>New Task</Text>
          <Pressable style={[styles.submitBtn, { backgroundColor: theme.accent }]} onPress={storeData}>
            <FontAwesome5 name="check" size={18} color="white" />
          </Pressable>
        </View>

        <View style={styles.content}>
          {/* Type Selector */}
          <Text style={[styles.label, { color: theme.subtext }]}>Category</Text>
          <View style={[styles.typeContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
            <Pressable 
              onPress={() => setType('daily')}
              style={[styles.typeBtn, type === 'daily' && { backgroundColor: theme.accent }]}
            >
              <Text style={[styles.typeText, { color: type === 'daily' ? 'white' : theme.subtext }]}>Daily</Text>
            </Pressable>
            <Pressable 
              onPress={() => {
                setType('long-term');
                setIsRecurring(false);
              }}
              style={[styles.typeBtn, type === 'long-term' && { backgroundColor: theme.accent }]}
            >
              <Text style={[styles.typeText, { color: type === 'long-term' ? 'white' : theme.subtext }]}>Long Term</Text>
            </Pressable>
          </View>

          {/* Title Input */}
          <Text style={[styles.label, { color: theme.subtext }]}>Title</Text>
          <TextInput
            placeholder="What's on your mind?"
            value={title}
            onChangeText={setTitle}
            placeholderTextColor={theme.subtext + '80'}
            style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          />

          {/* Conditional Deadline */}
          {type === 'long-term' && (
            <>
              <Text style={[styles.label, { color: theme.subtext }]}>Deadline</Text>
              <Pressable 
                style={[styles.input, styles.dateRow, { backgroundColor: theme.card, borderColor: theme.border }]} 
                onPress={showDatePicker}
              >
                <Text style={{ color: theme.title, fontWeight: '500' }}>
                  {date.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })} — {date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </Text>
                <Ionicons name="calendar-outline" size={20} color={theme.accent} />
              </Pressable>
            </>
          )}

          {/* Description */}
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

          {/* Recurring Toggle */}
          {type === 'daily' && (
            <View style={[styles.recurringBox, { backgroundColor: theme.background }]}>
              <View style={styles.recurringLabelGroup}>
                <Text style={[styles.recurringTitle, { color: theme.subtext }]}>Recurring Mode</Text>
              </View>
              <Switch
                value={isRecurring}
                onValueChange={setIsRecurring}
                trackColor={{ false: theme.border, true: theme.accent + '80' }}
                thumbColor={isRecurring ? theme.accent : '#f4f3f4'}
              />
            </View>
          )}
          
          {type === 'daily' && (
            <View style={styles.infoBox}>
              <Ionicons name="time-outline" size={16} color={theme.subtext} />
              <Text style={[styles.infoText, { color: theme.subtext }]}>
                {isRecurring ? "This task will repeat every day." : "This task will clear automatically tomorrow at 6:00 AM."}
              </Text>
            </View>
          )}
        </View>

        <DateTimePickerModal
          isVisible={isDatePickerVisible}
          mode="datetime"
          onConfirm={handleConfirm}
          onCancel={hideDatePicker}
          date={date}
          isDarkModeEnabled={colorScheme === 'dark'}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: "row", paddingHorizontal: 20, paddingVertical: 20, justifyContent: "space-between", alignItems: "center" },
  headerTitle: { fontSize: 20, fontWeight: "800" },
  backBtn: { width: 40, height: 40, justifyContent: 'center' },
  submitBtn: { width: 40, height: 40, borderRadius: 20, justifyContent: 'center', alignItems: 'center', elevation: 3, shadowColor: '#000', shadowOpacity: 0.1, shadowRadius: 5, shadowOffset: { width: 0, height: 2 }},
  content: { paddingHorizontal: 20 },
  label: { fontSize: 13, fontWeight: "700", marginBottom: 8, marginTop: 20, marginLeft: 4 },
  input: { borderRadius: 15, padding: 16, fontSize: 16, fontWeight: '500', borderWidth: 1 },
  textArea: { minHeight: 120 },
  dateRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  typeContainer: { flexDirection: 'row', borderRadius: 15, padding: 4, borderWidth: 1 },
  typeBtn: { flex: 1, paddingVertical: 10, borderRadius: 12, alignItems: 'center' },
  typeText: { fontWeight: '700', fontSize: 14 },
  recurringBox: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 5, },
  recurringLabelGroup: { flexDirection: 'row', alignItems: 'center' },
  recurringTitle: { fontSize: 14, fontWeight: '700' },
  infoBox: { flexDirection: 'row', alignItems: 'center', marginTop: 15, gap: 6, paddingHorizontal: 4 },
  infoText: { fontSize: 12, fontWeight: '500' },
});