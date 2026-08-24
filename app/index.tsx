import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useRef, useState } from "react";
import {
  Animated,
  Modal,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  useColorScheme
} from "react-native";
import { requestWidgetUpdate } from 'react-native-android-widget';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';
import { MyWidget } from './widget';

// --- TYPES ---
interface Task {
  id: string;
  title: string;
  description: string;
  completed: boolean;
  date: string;
  startTime?: string;
  endTime?: string;
  createdDate?: string;
}

// --- UTILS ---
const isSameDay = (d1: Date, d2: Date) => {
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};

const generateMonthGrid = (currentMonthDate: Date) => {
  const year = currentMonthDate.getFullYear();
  const month = currentMonthDate.getMonth();

  const firstDayOfMonth = new Date(year, month, 1);
  const startingDayOfWeek = firstDayOfMonth.getDay();

  const startDate = new Date(firstDayOfMonth);
  startDate.setDate(startDate.getDate() - startingDayOfWeek);

  const grid = [];
  const curr = new Date(startDate);

  for (let i = 0; i < 35; i++) {
    grid.push(new Date(curr));
    curr.setDate(curr.getDate() + 1);
  }

  return grid;
};

// Formats date to "August 13"
const formatSectionHeaderDate = (date: Date) => {
  return date.toLocaleDateString('en-US', { month: 'long', day: 'numeric' });
};

const formatTimeStr = (isoString?: string) => {
  if (!isoString) return null;
  const d = new Date(isoString);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true }).toLowerCase();
};

// --- TASK CARD COMPONENT ---
const AnimatedTaskCard = ({ 
  item, 
  onToggle, 
  onDelete, 
  theme, 
  isEditMode 
}: { 
  item: Task; 
  onToggle: (id: string) => void; 
  onDelete: (id: string) => void; 
  theme: any;
  isEditMode: boolean;
}) => {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => Animated.spring(scaleAnim, { toValue: 0.98, useNativeDriver: true }).start();
  const handlePressOut = () => Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true }).start();

  const timeFormatted = formatTimeStr(item.startTime) || formatTimeStr(item.endTime) || "All day";

  return (
    <Animated.View style={[styles.itemRow, { transform: [{ scale: scaleAnim }] }]}>
      {/* CHECKBOX */}
      <Pressable onPress={() => onToggle(item.id)} style={styles.checkContainer}>
        {item.completed ? (
          <View style={[styles.statusCircle, { backgroundColor: '#2ECC71' }]}>
            <FontAwesome5 name="check" size={10} color="#FFF" />
          </View>
        ) : (
          <View style={[styles.statusCircleOutline, { borderColor: theme.border || '#DDD' }]} />
        )}
      </Pressable>

      {/* TASK CONTENT */}
      <Pressable 
        style={styles.textContainer}
        onPressIn={handlePressIn}
        onPressOut={handlePressOut}
        onPress={() => onToggle(item.id)}
      >
        <Text style={[styles.taskTitle, { color: theme.title }, item.completed && styles.textCrossed]}>
          {item.title}
        </Text>
        {item.description ? (
          <Text style={[styles.taskSub, { color: theme.subtext }]} numberOfLines={1}>
            {item.description}
          </Text>
        ) : null}
      </Pressable>

      {/* TIME OR DELETE BUTTON */}
      <View style={styles.rightActionContainer}>
        {isEditMode ? (
          <Pressable onPress={() => onDelete(item.id)} style={styles.deleteBtn}>
            <FontAwesome5 name="trash-alt" size={15} color="#FF4D4D" />
          </Pressable>
        ) : (
          <Text style={[styles.timeText, { color: theme.subtext }]}>
            {timeFormatted}
          </Text>
        )}
      </View>
    </Animated.View>
  );
};

// --- MAIN SCREEN ---
export default function Index() {
  const [data, setData] = useState<Task[]>([]);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [viewDate, setViewDate] = useState<Date>(new Date());
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [isEditMode, setIsEditMode] = useState(false);

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  const KEY = 'dbKey';
  const monthGrid = generateMonthGrid(viewDate);

  const changeMonth = (increment: number) => {
    const newDate = new Date(viewDate);
    newDate.setMonth(newDate.getMonth() + increment);
    setViewDate(newDate);
  };

  const reloadItems = async () => {
    const jsonData = await AsyncStorage.getItem(KEY);
    if (jsonData) {
      const parsed: Task[] = JSON.parse(jsonData);
      setData(parsed);
      updateWidget(parsed);
    }
  };

  useFocusEffect(useCallback(() => { reloadItems(); }, []));

  const updateWidget = async (currentTasks: Task[]) => {
    const formattedDate = new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
    const widgetTasks = currentTasks.filter(t => !t.completed);
    await AsyncStorage.setItem("widget", JSON.stringify(widgetTasks));
    requestWidgetUpdate({
      widgetName: 'widget',
      renderWidget: () => <MyWidget task={widgetTasks} date={formattedDate}/>,
      widgetNotFound: () => {}
    });
  };

  const toggleComplete = async (id: string) => {
    const newArray = data.map(item => {
      if (item.id === id) {
        return { ...item, completed: !item.completed };
      }
      return item;
    });
    setData(newArray);
    await AsyncStorage.setItem(KEY, JSON.stringify(newArray));
    updateWidget(newArray);
  };

  const deleteItem = async () => {
    if (!deleteId) return;
    const newArray = data.filter(item => item.id !== deleteId);
    setData(newArray);
    await AsyncStorage.setItem(KEY, JSON.stringify(newArray));
    setDeleteModalVisible(false);
    setDeleteId(null);
    updateWidget(newArray);
  };

  const getDayTaskTextColor = (date: Date, isCurrentMonth: boolean) => {
    const dayTasks = data.filter(t => isSameDay(new Date(t.date), date));
    
    if (dayTasks.length === 0) {
      return isCurrentMonth ? theme.text : '#C5C5C5';
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);

    const hasLate = dayTasks.some(t => !t.completed && checkDate < today);
    if (hasLate) return '#FF4D4D';

    const hasPending = dayTasks.some(t => !t.completed && checkDate >= today);
    if (hasPending) return '#3A86FF';

    const allCompleted = dayTasks.length > 0 && dayTasks.every(t => t.completed);
    if (allCompleted) return '#2ECC71';

    return isCurrentMonth ? theme.subtext : '#C5C5C5';
  };

  // Filter tasks belonging exclusively to the selected date
  const selectedDateTasks = data.filter(task => isSameDay(new Date(task.date), selectedDate));

  const daysOfWeek = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      {/* CALENDAR SECTION */}
      <View style={styles.calendarSection}>
        <View style={styles.header}>
          <Text style={[styles.mainTitle, { color: theme.title }]}> Tasks</Text>
          <Pressable style={styles.addBtnBlue} onPress={() => router.push('/addtask')}>
            <FontAwesome5 name="plus" size={14} color="white" />
          </Pressable>
        </View>

        <View style={styles.monthHeader}>
          <Text style={[styles.monthTitle, { color: theme.subtext }]}>
            {viewDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
          </Text>
          <View style={styles.monthNavBtns}>
            <Pressable onPress={() => changeMonth(-1)} style={styles.arrowBtn}>
              <FontAwesome5 name="chevron-left" size={12} color={theme.subtext} />
            </Pressable>
            <Pressable onPress={() => changeMonth(1)} style={styles.arrowBtn}>
              <FontAwesome5 name="chevron-right" size={12} color={theme.subtext} />
            </Pressable>
          </View>
        </View>

        <View style={styles.weekRow}>
          {daysOfWeek.map((day, index) => (
            <Text key={index} style={[styles.weekDayText, { color: theme.subtext }]}>{day}</Text>
          ))}
        </View>

        <View style={styles.grid}>
          {monthGrid.map((dateItem, idx) => {
            const isCurrentMonth = dateItem.getMonth() === viewDate.getMonth();
            const isSelected = isSameDay(dateItem, selectedDate);
            const isToday = isSameDay(dateItem, new Date());
            const dateTextColor = getDayTaskTextColor(dateItem, isCurrentMonth);

            return (
              <Pressable
                key={idx}
                onPress={() => {
                  setSelectedDate(dateItem);
                  if (dateItem.getMonth() !== viewDate.getMonth()) {
                    setViewDate(dateItem);
                  }
                }}
                style={styles.dayCell}
              >
                <View style={[
                  styles.dayNumContainer,
                  isSelected && styles.selectedDayCircle,
                  isToday && !isSelected && styles.todayOutlineCircle
                ]}>
                  <Text style={[
                    styles.dayNumText,
                    { color: dateTextColor },
                    isSelected && { fontWeight: '800' }
                  ]}>
                    {dateItem.getDate()}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </View>
      </View>

      {/* TASKS LIST SECTION FOR SELECTED DATE ONLY */}
      <View style={[
        styles.taskContainerSheet, 
        { backgroundColor: theme.card || (colorScheme === 'dark' ? '#1E1E1E' : '#FFFFFF') }
      ]}>
        {/* HEADER WITH FORMATTED SELECTED DATE */}
        <View style={styles.sheetHeaderContainer}>
          <Text style={[styles.sheetDateTitle, { color: theme.title }]}>
            {formatSectionHeaderDate(selectedDate)}
          </Text>
          <Pressable 
            style={styles.editToggleBtn} 
            onPress={() => setIsEditMode(!isEditMode)}
          >
            <FontAwesome5 
              name={isEditMode ? "check" : "pencil-alt"} 
              size={15} 
              color={isEditMode ? "#2ECC71" : theme.subtext} 
            />
          </Pressable>
        </View>

        <ScrollView 
          showsVerticalScrollIndicator={false} 
          contentContainerStyle={{ paddingBottom: 100 }}
        >
          {selectedDateTasks.length === 0 ? (
            <View style={styles.emptyContainer}>
              <FontAwesome5 name="calendar-check" size={36} color={theme.border} />
              <Text style={[styles.emptyText, { color: theme.subtext }]}>No tasks for this day</Text>
            </View>
          ) : (
            <View style={styles.dateSection}>
              {selectedDateTasks.map(item => (
                <AnimatedTaskCard
                  key={item.id}
                  item={item}
                  theme={theme}
                  isEditMode={isEditMode}
                  onToggle={toggleComplete}
                  onDelete={(id: string) => { setDeleteId(id); setDeleteModalVisible(true); }}
                />
              ))}
            </View>
          )}
        </ScrollView>
      </View>

      <Footer />

      {/* DELETE MODAL */}
      <Modal transparent visible={deleteModalVisible} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Task</Text>
            <Text style={[styles.modalSub, { color: theme.subtext }]}>Remove this task permanently?</Text>
            <View style={styles.modalActions}>
              <Pressable style={styles.modalBtn} onPress={() => setDeleteModalVisible(false)}>
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable style={[styles.modalBtn, { backgroundColor: theme.delete || '#FF4D4D' }]} onPress={deleteItem}>
                <Text style={{ color: 'white', fontWeight: '600' }}>Delete</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  calendarSection: { paddingHorizontal: 22, paddingTop: 10, paddingBottom: 10 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  mainTitle: { fontSize: 26, fontWeight: '800', letterSpacing: -0.5 },
  addBtnBlue: { 
    width: 36, 
    height: 36, 
    borderRadius: 18, 
    backgroundColor: '#3A86FF', 
    justifyContent: 'center', 
    alignItems: 'center',
    shadowColor: '#3A86FF',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 3,
  },
  monthHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
    marginTop: 4,
  },
  monthTitle: { fontSize: 15, fontWeight: '700', letterSpacing: -0.2 },
  monthNavBtns: { flexDirection: 'row', gap: 8 },
  arrowBtn: { padding: 6, borderRadius: 6 },
  weekRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  weekDayText: { width: '14.28%', textAlign: 'center', fontSize: 12, fontWeight: '600' },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  dayCell: { width: '14.28%', height: 38, justifyContent: 'center', alignItems: 'center' },
  dayNumContainer: { width: 30, height: 30, borderRadius: 15, justifyContent: 'center', alignItems: 'center' },
  selectedDayCircle: { borderBottomWidth: 2, borderColor: '#3A86FF', backgroundColor: 'rgba(58, 134, 255, 0.08)' },
  todayOutlineCircle: { borderWidth: 1, borderColor: '#A0A0A0' },
  dayNumText: { fontSize: 13, fontWeight: '700' },
  
  /* BELOW CALENDAR SHEET STYLES */
  taskContainerSheet: { 
    flex: 1, 
    borderTopLeftRadius: 32, 
    borderTopRightRadius: 32, 
    paddingHorizontal: 24, 
    paddingTop: 24,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -3 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
  },
  sheetHeaderContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sheetDateTitle: {
    fontSize: 16, // Reduced font size
    fontWeight: '700',
    letterSpacing: -0.2,
  },
  editToggleBtn: {
    padding: 6,
    borderRadius: 8,
  },
  dateSection: { marginBottom: 12 },
  itemRow: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    marginVertical: 12 
  },
  checkContainer: {
    marginRight: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusCircle: {
    width: 22,
    height: 22,
    borderRadius: 11,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusCircleOutline: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
  },
  textContainer: { flex: 1, paddingRight: 12 },
  taskTitle: { fontSize: 15, fontWeight: '700', letterSpacing: -0.2 },
  taskSub: { fontSize: 12, marginTop: 2, opacity: 0.6 },
  textCrossed: { textDecorationLine: 'line-through', opacity: 0.4 },
  rightActionContainer: {
    minWidth: 60,
    alignItems: 'flex-end',
    justifyContent: 'center',
  },
  timeText: { fontSize: 12, fontWeight: '500', opacity: 0.7 },
  deleteBtn: { padding: 6 },
  emptyContainer: { alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
  emptyText: { marginTop: 10, fontSize: 14, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', padding: 22, borderRadius: 24, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 8 },
  modalSub: { textAlign: 'center', marginBottom: 20, fontSize: 13 },
  modalActions: { flexDirection: 'row', gap: 12 },
  modalBtn: { flex: 1, padding: 12, borderRadius: 12, alignItems: 'center' },
});