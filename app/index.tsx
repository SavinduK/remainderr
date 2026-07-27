import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useEffect, useRef, useState } from "react";
import { Animated, Modal, Pressable, ScrollView, StatusBar, StyleSheet, Text, View, useColorScheme } from "react-native";
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
  date: string; // Used as the Due Date for long-term tasks or task date
  createdDate?: string; // Creation timestamp for checking start date
  type: 'daily' | 'long-term';
  recurring: boolean;
}

interface DateItem {
  date: Date;
  dateString: string; 
  dayName: string;
  dayNumber: number;
}

// --- UTILS ---
const isDailyTaskValid = (taskDate: string) => {
  const now = new Date();
  const taskCreation = new Date(taskDate);
  const cutoff = new Date();
  cutoff.setHours(6, 0, 0, 0);

  if (now.getHours() < 6) {
    cutoff.setDate(cutoff.getDate() - 1);
  }
  return taskCreation >= cutoff;
};

// Generates an array of dates: 1 month back to 1 month forward
const generateDateRange = (): DateItem[] => {
  const dates: DateItem[] = [];
  const today = new Date();
  
  const start = new Date(today);
  start.setMonth(start.getMonth() - 1);

  const end = new Date(today);
  end.setMonth(end.getMonth() + 1);

  const current = new Date(start);
  while (current <= end) {
    dates.push({
      date: new Date(current),
      dateString: current.toISOString().split('T')[0],
      dayName: current.toLocaleDateString('en-US', { weekday: 'short' }),
      dayNumber: current.getDate(),
    });
    current.setDate(current.getDate() + 1);
  }
  return dates;
};

const isSameDay = (d1: Date, d2: Date) => {
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};

// Checks if targetDate is between startDate and endDate (at midnight granularity)
const isDateInRange = (targetDate: Date, startDate: Date, endDate: Date) => {
  const target = new Date(targetDate).setHours(0, 0, 0, 0);
  const start = new Date(startDate).setHours(0, 0, 0, 0);
  const end = new Date(endDate).setHours(23, 59, 59, 999);

  return target >= start && target <= end;
};

// --- COMPONENTS ---
const AnimatedTaskCard = ({ item, onToggle, onDelete, isOverdue, theme }: any) => {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => Animated.spring(scaleAnim, { toValue: 0.98, useNativeDriver: true }).start();
  const handlePressOut = () => Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true }).start();

  const itemDate = new Date(item.date);
  const taskDateLabel = itemDate.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

  return (
    <Animated.View style={[styles.item, { backgroundColor: theme.card, borderColor: theme.border, transform: [{ scale: scaleAnim }] }]}>
      <View style={styles.row}>
        <Pressable 
          onPressIn={handlePressIn}
          onPressOut={handlePressOut}
          onPress={() => onToggle(item.id)} 
          style={[styles.checkbox, { borderColor: item.completed ? theme.success : theme.border }, item.completed && { backgroundColor: theme.success }]}
        >
          {item.completed && <FontAwesome5 name="check" size={12} color="white" />}
        </Pressable>

        <View style={styles.textContainer}>
          <View style={styles.titleRow}>
             <Text style={[styles.largeText, { color: theme.title }, item.completed && styles.textCrossed]}>{item.title}</Text>
          </View>
          {item.description && <Text style={[styles.smallText, { color: theme.subtext }]}>{item.description}</Text>}
          <Text style={[styles.dateLabel, { color: isOverdue && !item.completed ? theme.delete : theme.subtext }]}>
            {item.type === 'daily' ? (item.recurring ? 'Daily Recurring' : 'Daily Goal') : `Due: ${taskDateLabel}`}
          </Text>
        </View>

        <Pressable onPress={() => onDelete(item.id)} style={styles.deleteBtn}>
          <FontAwesome5 name="trash" size={16} color={theme.icon} />
        </Pressable>
      </View>
    </Animated.View>
  );
};

// --- MAIN SCREEN ---
export default function Index() {
  const [data, setData] = useState<Task[]>([]);
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [dateList, setDateList] = useState<DateItem[]>([]);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  
  const dateStripRef = useRef<ScrollView>(null);
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();
  const KEY = 'dbKey';
  const HISTORY_KEY = 'historyKey';

  useEffect(() => {
    const dates = generateDateRange();
    setDateList(dates);

    const todayIndex = dates.findIndex(d => isSameDay(d.date, new Date()));
    if (todayIndex !== -1) {
      setTimeout(() => {
        dateStripRef.current?.scrollTo({ x: todayIndex * 62 - 150, animated: true });
      }, 300);
    }
  }, []);

  const updateHistoryEntry = async (task: Task, action: 'update' | 'delete') => {
    try {
      const historyData = await AsyncStorage.getItem(HISTORY_KEY);
      let history = historyData ? JSON.parse(historyData) : {};
      const dateKey = new Date(task.date).toISOString().split('T')[0];

      if (!history[dateKey]) history[dateKey] = { completed: [], incomplete: [] };

      history[dateKey].completed = history[dateKey].completed.filter((t: string) => t !== task.title);
      history[dateKey].incomplete = history[dateKey].incomplete.filter((t: string) => t !== task.title);

      if (action === 'update') {
        if (task.completed) history[dateKey].completed.push(task.title);
        else history[dateKey].incomplete.push(task.title);
      }

      await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(history));
    } catch (e) { console.error(e); }
  };

  const reloadItems = async () => {
    const jsonData = await AsyncStorage.getItem(KEY);
    if (jsonData) {
      const parsed: Task[] = JSON.parse(jsonData);
      let hasChanges = false;

      const processedTasks = parsed.map(task => {
        if (task.type === 'daily' && !isDailyTaskValid(task.date)) {
          if (task.recurring) {
            hasChanges = true;
            return { ...task, completed: false, date: new Date().toISOString() };
          }
          hasChanges = true;
          return null;
        }
        return task;
      }).filter((t): t is Task => t !== null);

      if (hasChanges) {
        await AsyncStorage.setItem(KEY, JSON.stringify(processedTasks));
      }

      setData(processedTasks);
      updateWidget(processedTasks);
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
        const updated = { ...item, completed: !item.completed };
        if (item.type === 'daily') updateHistoryEntry(updated, 'update');
        return updated;
      }
      return item;
    });
    setData(newArray);
    await AsyncStorage.setItem(KEY, JSON.stringify(newArray));
    updateWidget(newArray);
  };

  const deleteItem = async () => {
    const taskToDelete = data.find(t => t.id === deleteId);
    if (taskToDelete?.type === 'daily') await updateHistoryEntry(taskToDelete, 'delete');

    const newArray = data.filter(item => item.id !== deleteId);
    setData(newArray);
    await AsyncStorage.setItem(KEY, JSON.stringify(newArray));
    setDeleteModalVisible(false);
    updateWidget(newArray);
  };

  // Filter tasks based on selected date
  const visibleTasks = data.filter(task => {
    const taskDueDate = new Date(task.date);

    if (task.type === 'daily') {
      if (task.recurring) return true;
      return isSameDay(taskDueDate, selectedDate);
    }

    if (task.type === 'long-term') {
      // Show long-term tasks across all days between creation date and due date
      const creationDate = task.createdDate ? new Date(task.createdDate) : taskDueDate;
      return isDateInRange(selectedDate, creationDate, taskDueDate);
    }

    return false;
  });

  const dailyTasks = visibleTasks.filter(t => t.type === 'daily');
  const longTermTasks = visibleTasks.filter(t => t.type === 'long-term');

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />
      
      {/* HEADER */}
      <View style={styles.header}>
        <View>
          <Text style={[styles.headerSubtitle, { color: theme.subtext }]}>
            {selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
          </Text>
          <Text style={[styles.headerTitle, { color: theme.title }]}>My Tasks</Text>
        </View>

        <View style={styles.headerActions}>
          <Pressable style={[styles.addBtn, { backgroundColor: theme.accent }]} onPress={() => router.push('/addtask')}>
            <FontAwesome5 name="plus" size={18} color="white" />
          </Pressable>
        </View>
      </View>

      {/* HORIZONTAL DATE STRIP */}
      <View style={styles.dateStripContainer}>
        <ScrollView 
          ref={dateStripRef}
          horizontal 
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.dateStripContent}
        >
          {dateList.map((item, index) => {
            const isSelected = isSameDay(item.date, selectedDate);

            return (
              <Pressable
                key={index}
                onPress={() => setSelectedDate(item.date)}
                style={[
                  styles.dateCard,
                  { backgroundColor: theme.card, borderColor: theme.border },
                  isSelected && { backgroundColor: theme.accent, borderColor: theme.accent },
                ]}
              >
                <Text style={[
                  styles.dateDayName, 
                  { color: theme.subtext },
                  isSelected && { color: 'white', fontWeight: '700' }
                ]}>
                  {item.dayName}
                </Text>
                <Text style={[
                  styles.dateDayNum, 
                  { color: theme.title },
                  isSelected && { color: 'white' }
                ]}>
                  {item.dayNumber}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
      </View>

      {/* TASK LIST */}
      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        {visibleTasks.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="feather" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No tasks scheduled for this day.</Text>
          </View>
        ) : (
          <>
            {dailyTasks.length > 0 && (
              <>
                <Text style={[styles.sectionTitle, { color: theme.accent }]}>Daily Goals</Text>
                {dailyTasks.map(item => (
                  <AnimatedTaskCard key={item.id} item={item} theme={theme} onToggle={toggleComplete} onDelete={(id: string) => { setDeleteId(id); setDeleteModalVisible(true); }} />
                ))}
              </>
            )}
            {longTermTasks.length > 0 && (
              <>
                <Text style={[styles.sectionTitle, { color: theme.accent, marginTop: 20 }]}>Long Term</Text>
                {longTermTasks.map(item => (
                  <AnimatedTaskCard key={item.id} item={item} theme={theme} onToggle={toggleComplete} isOverdue={!item.completed && new Date(item.date) < new Date()} onDelete={(id: string) => { setDeleteId(id); setDeleteModalVisible(true); }} />
                ))}
              </>
            )}
          </>
        )}
      </ScrollView>

      <Footer />

      {/* DELETE MODAL */}
      <Modal transparent visible={deleteModalVisible} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Task</Text>
            <Text style={[styles.modalSub, { color: theme.subtext }]}>Remove this from today and your history log?</Text>
            <View style={styles.modalActions}>
              <Pressable style={styles.modalBtn} onPress={() => setDeleteModalVisible(false)}>
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable style={[styles.modalBtn, { backgroundColor: theme.delete }]} onPress={deleteItem}>
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
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 25, paddingTop: 15, paddingBottom: 10 },
  headerSubtitle: { fontSize: 14, fontWeight: '500', marginBottom: 4 },
  headerTitle: { fontSize: 28, fontWeight: '800', letterSpacing: -0.5 },
  headerActions: { flexDirection: 'row', alignItems: 'center' },
  
  dateStripContainer: { marginBottom: 15 },
  dateStripContent: { paddingHorizontal: 20, gap: 10 },
  dateCard: {
    width: 52,
    height: 68,
    borderRadius: 16,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 8,
  },
  dateDayName: { fontSize: 12, fontWeight: '500', marginBottom: 4 },
  dateDayNum: { fontSize: 18, fontWeight: '700' },

  sectionTitle: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 12, marginLeft: 5 },
  addBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  scroll: { flex: 1, paddingHorizontal: 20 },
  item: { padding: 16, borderRadius: 20, marginBottom: 12, borderWidth: 1 },
  row: { flexDirection: 'row', alignItems: 'center' },
  checkbox: { width: 24, height: 24, borderRadius: 12, borderWidth: 2, marginRight: 15, justifyContent: 'center', alignItems: 'center' },
  textContainer: { flex: 1 },
  titleRow: { flexDirection: 'row', alignItems: 'center' },
  largeText: { fontSize: 16, fontWeight: '600' },
  smallText: { fontSize: 13, marginTop: 2 },
  dateLabel: { fontSize: 11, fontWeight: '600', marginTop: 6 },
  textCrossed: { textDecorationLine: 'line-through', opacity: 0.4 },
  deleteBtn: { padding: 8 },
  emptyContainer: { alignItems: 'center', marginTop: 80 },
  emptyText: { marginTop: 15, fontSize: 16, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '85%', padding: 25, borderRadius: 30, alignItems: 'center' },
  modalTitle: { fontSize: 20, fontWeight: '700', marginBottom: 10 },
  modalSub: { textAlign: 'center', marginBottom: 25 },
  modalActions: { flexDirection: 'row', gap: 15 },
  modalBtn: { flex: 1, padding: 15, borderRadius: 15, alignItems: 'center' },
});