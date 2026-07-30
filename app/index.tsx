import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useRef, useState } from "react";
import {
  Animated,
  LayoutChangeEvent,
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
  createdDate?: string;
  type: 'daily' | 'long-term';
  recurring: boolean;
}

interface GroupedTasks {
  dateStr: string; // YYYY-MM-DD
  dateObj: Date;
  tasks: Task[];
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

const isSameDay = (d1: Date, d2: Date) => {
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};

// Generates 35 days (5 weeks) for a month grid view
const generateMonthGrid = (currentMonthDate: Date) => {
  const year = currentMonthDate.getFullYear();
  const month = currentMonthDate.getMonth();

  const firstDayOfMonth = new Date(year, month, 1);
  const startingDayOfWeek = firstDayOfMonth.getDay(); // 0 = Sun

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

const formatSectionHeaderDate = (date: Date) => {
  const weekday = date.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();
  const dayNum = date.getDate();
  return `${weekday} ${dayNum}`;
};

// --- REDESIGNED TASK ROW COMPONENT ---
const AnimatedTaskCard = ({ item, onToggle, onDelete, theme }: any) => {
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => Animated.spring(scaleAnim, { toValue: 0.98, useNativeDriver: true }).start();
  const handlePressOut = () => Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true }).start();

  const itemDate = new Date(item.date);
  const timeFormatted = itemDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  const isDaily = item.type === 'daily';

  return (
    <Animated.View style={[styles.itemRow, { transform: [{ scale: scaleAnim }] }]}>
      {/* TIME COLUMN */}
      <View style={styles.timeColumn}>
        <Text style={[styles.timeText, { color: theme.subtext }]}>
          {isDaily ? 'All day' : timeFormatted}
        </Text>
        {!isDaily && (
          <Text style={[styles.timeTextSub, { color: theme.subtext }]}>
            {item.recurring ? 'Daily' : 'Due'}
          </Text>
        )}
      </View>

      {/* COLOR ACCENT DOT */}
      <View style={[styles.dot, { backgroundColor: theme.background }]} />

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

      {/* DELETE BUTTON */}
      <Pressable onPress={() => onDelete(item.id)} style={styles.deleteBtn}>
        <FontAwesome5 name="trash-alt" size={13} color={theme.subtext} />
      </Pressable>
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

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  const scrollViewRef = useRef<ScrollView>(null);
  const sectionYPositions = useRef<{ [key: string]: number }>({});
  const hasAutoScrolled = useRef(false);

  const KEY = 'dbKey';
  const HISTORY_KEY = 'historyKey';

  const monthGrid = generateMonthGrid(viewDate);

  const changeMonth = (increment: number) => {
    const newDate = new Date(viewDate);
    newDate.setMonth(newDate.getMonth() + increment);
    setViewDate(newDate);
  };

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

  // Group all tasks by date in chronological order
  const getGroupedTasks = (): GroupedTasks[] => {
    const groupsMap: { [key: string]: { dateObj: Date; tasks: Task[] } } = {};

    data.forEach(task => {
      const taskDate = new Date(task.date);
      const dateKey = taskDate.toISOString().split('T')[0];

      if (!groupsMap[dateKey]) {
        groupsMap[dateKey] = { dateObj: taskDate, tasks: [] };
      }
      groupsMap[dateKey].tasks.push(task);
    });

    return Object.keys(groupsMap)
      .sort((a, b) => new Date(a).getTime() - new Date(b).getTime())
      .map(key => ({
        dateStr: key,
        dateObj: groupsMap[key].dateObj,
        tasks: groupsMap[key].tasks,
      }));
  };

  const groupedTasks = getGroupedTasks();
  const todayStr = new Date().toISOString().split('T')[0];

  const handleSectionLayout = (dateStr: string, event: LayoutChangeEvent) => {
    const y = event.nativeEvent.layout.y;
    sectionYPositions.current[dateStr] = y;

    if (!hasAutoScrolled.current && groupedTasks.length > 0) {
      const targetGroup = groupedTasks.find(g => g.dateStr >= todayStr) || groupedTasks[0];
      if (targetGroup && sectionYPositions.current[targetGroup.dateStr] !== undefined) {
        hasAutoScrolled.current = true;
        setTimeout(() => {
          scrollViewRef.current?.scrollTo({
            y: sectionYPositions.current[targetGroup.dateStr],
            animated: true,
          });
        }, 100);
      }
    }
  };

  const scrollToDateSection = (dateItem: Date) => {
    const matchingGroup = groupedTasks.find(g => isSameDay(g.dateObj, dateItem));
    
    if (matchingGroup && sectionYPositions.current[matchingGroup.dateStr] !== undefined) {
      scrollViewRef.current?.scrollTo({
        y: sectionYPositions.current[matchingGroup.dateStr],
        animated: true,
      });
    } else {
      const selectedTime = dateItem.getTime();
      const nextGroup = groupedTasks.find(g => g.dateObj.getTime() >= selectedTime);
      
      if (nextGroup && sectionYPositions.current[nextGroup.dateStr] !== undefined) {
        scrollViewRef.current?.scrollTo({
          y: sectionYPositions.current[nextGroup.dateStr],
          animated: true,
        });
      }
    }
  };

  const daysOfWeek = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      {/* TOP CALENDAR SECTION */}
      <View style={styles.calendarSection}>
        {/* HEADER */}
        <View style={styles.header}>
          <Text style={[styles.mainTitle, { color: theme.title }]}>
            My Tasks
          </Text>

          <Pressable style={styles.addBtnBlue} onPress={() => router.push('/addtask')}>
            <FontAwesome5 name="plus" size={14} color="white" />
          </Pressable>
        </View>

        {/* MONTH & YEAR HEADER WITH NAVIGATION */}
        <View style={styles.monthHeader}>
          <Text style={[styles.monthTitle, { color: theme.title }]}>
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

        {/* WEEKDAYS HEADER */}
        <View style={styles.weekRow}>
          {daysOfWeek.map((day, index) => (
            <Text key={index} style={[styles.weekDayText, { color: theme.subtext }]}>{day}</Text>
          ))}
        </View>

        {/* CALENDAR DAYS GRID */}
        <View style={styles.grid}>
          {monthGrid.map((dateItem, idx) => {
            const isCurrentMonth = dateItem.getMonth() === viewDate.getMonth();
            const isSelected = isSameDay(dateItem, selectedDate);
            const isToday = isSameDay(dateItem, new Date());

            return (
              <Pressable
                key={idx}
                onPress={() => {
                  setSelectedDate(dateItem);
                  if (dateItem.getMonth() !== viewDate.getMonth()) {
                    setViewDate(dateItem);
                  }
                  scrollToDateSection(dateItem);
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
                    { color: isCurrentMonth ? theme.title : '#C5C5C5' },
                    isSelected && { color: theme.title, fontWeight: '700' }
                  ]}>
                    {dateItem.getDate()}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </View>
      </View>

      {/* CHRONOLOGICAL MULTI-DAY TASKS LIST CONTAINER */}
      <View style={[styles.taskContainer, { backgroundColor: theme.background }]}>
        <ScrollView 
          ref={scrollViewRef}
          showsVerticalScrollIndicator={false} 
          contentContainerStyle={{ paddingBottom: 110 }}
        >
          {groupedTasks.length === 0 ? (
            <View style={styles.emptyContainer}>
              <FontAwesome5 name="calendar-check" size={36} color={theme.border} />
              <Text style={[styles.emptyText, { color: theme.subtext }]}>No tasks scheduled</Text>
            </View>
          ) : (
            groupedTasks.map(group => (
              <View 
                key={group.dateStr}
                onLayout={(e) => handleSectionLayout(group.dateStr, e)}
                style={styles.dateSection}
              >
                {/* REDESIGNED HEADER: DAY/DATE WITH DIVIDER LINE */}
                <View style={styles.sheetHeader}>
                  <Text style={[styles.sheetTitle, { color: theme.subtext }]}>
                    {formatSectionHeaderDate(group.dateObj)}
                  </Text>
                  <View style={[styles.headerDivider, { backgroundColor: theme.border }]} />
                </View>

                {/* REDESIGNED TASK LIST */}
                {group.tasks.map(item => (
                  <AnimatedTaskCard
                    key={item.id}
                    item={item}
                    theme={theme}
                    onToggle={toggleComplete}
                    onDelete={(id: string) => { setDeleteId(id); setDeleteModalVisible(true); }}
                  />
                ))}
              </View>
            ))
          )}
        </ScrollView>
      </View>

      {/* FOOTER */}
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
  
  // Header / Calendar Section
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

  // Month Header Bar
  monthHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
    marginTop: 4,
  },
  monthTitle: {
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: -0.2,
  },
  monthNavBtns: {
    flexDirection: 'row',
    gap: 8,
  },
  arrowBtn: {
    padding: 6,
    borderRadius: 6,
  },

  // Week Grid
  weekRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
  weekDayText: { width: '14.28%', textAlign: 'center', fontSize: 12, fontWeight: '600' },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  dayCell: { width: '14.28%', height: 38, justifyContent: 'center', alignItems: 'center' },
  dayNumContainer: { width: 30, height: 30, borderRadius: 15, justifyContent: 'center', alignItems: 'center' },
  selectedDayCircle: { borderBottomWidth: 2, borderColor: '#3A86FF', backgroundColor: 'rgba(58, 134, 255, 0.08)' },
  todayOutlineCircle: { borderWidth: 1, borderColor: '#A0A0A0' },
  dayNumText: { fontSize: 13, fontWeight: '500' },

  // Task Main Container
  taskContainer: {
    flex: 1,
    paddingHorizontal: 22,
    paddingTop: 15,
  },
  dateSection: { marginBottom: 28 },
  
  // Section Header Styles
  sheetHeader: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    marginBottom: 16 
  },
  sheetTitle: { 
    fontSize: 12, 
    fontWeight: '700', 
    letterSpacing: 0.8,
    marginRight: 12,
  },
  headerDivider: { 
    flex: 1, 
    height: 1, 
    opacity: 0.5 
  },

  // Task Row Styles
  itemRow: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    marginBottom: 16 
  },
  timeColumn: { 
    width: 52,
  },
  timeText: { 
    fontSize: 12, 
    fontWeight: '500', 
    lineHeight: 15, 
    opacity: 0.7 
  },
  timeTextSub: { 
    fontSize: 12, 
    fontWeight: '400', 
    lineHeight: 15, 
    opacity: 0.5 
  },
  dot: { 
    width: 7, 
    height: 7, 
    borderRadius: 3.5, 
    marginHorizontal: 12 
  },
  textContainer: { 
    flex: 1, 
    paddingRight: 8 
  },
  taskTitle: { 
    fontSize: 14, 
    fontWeight: '700', 
    letterSpacing: -0.2 
  },
  taskSub: { 
    fontSize: 12, 
    marginTop: 2, 
    opacity: 0.6 
  },
  textCrossed: { 
    textDecorationLine: 'line-through', 
    opacity: 0.4 
  },
  deleteBtn: { 
    paddingLeft: 10,
    paddingVertical: 6,
  },

  // Empty State
  emptyContainer: { alignItems: 'center', justifyContent: 'center', paddingTop: 40 },
  emptyText: { marginTop: 10, fontSize: 14, fontWeight: '500' },

  // Delete Modal
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '80%', padding: 22, borderRadius: 24, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 8 },
  modalSub: { textAlign: 'center', marginBottom: 20, fontSize: 13 },
  modalActions: { flexDirection: 'row', gap: 12 },
  modalBtn: { flex: 1, padding: 12, borderRadius: 12, alignItems: 'center' },
});