import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useMemo, useRef, useState } from "react";
import { Alert, Animated, FlatList, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View, useColorScheme } from "react-native";
import { GestureHandlerRootView, Swipeable } from 'react-native-gesture-handler';
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';

// --- TYPES ---
interface Transaction {
  id: string;
  title: string;
  amount: number;
  category: string;
  date: string;
}

interface GroupedTransactions {
  dateKey: string;
  formattedDate: string;
  dailyTotal: number;
  items: Transaction[];
}

interface CreditCard {
  id: string;
  bankName: string;
  cardNumber: string;
  expiry: string;
  type: 'Visa' | 'Mastercard';
  cardHolder: string;
  color: string;
}

const CATEGORIES: { [key: string]: { icon: string; color: string } } = {
  Food: { icon: 'utensils', color: '#FF9500' },
  Travel: { icon: 'car', color: '#5AC8FA' },
  Shopping: { icon: 'shopping-cart', color: '#FF2D55' },
  Health: { icon: 'heartbeat', color: '#4CD964' },
  Other: { icon: 'ellipsis-h', color: '#8E8E93' },
};

// --- SINGLE TRANSACTION ROW ---
const TransactionRow = ({ item, onDelete, theme, isLast }: any) => {
  const swipeableRef = useRef<Swipeable>(null);
  const scaleAnim = useRef(new Animated.Value(1)).current;

  const handlePressIn = () => Animated.spring(scaleAnim, { toValue: 0.98, useNativeDriver: true }).start();
  const handlePressOut = () => Animated.spring(scaleAnim, { toValue: 1, useNativeDriver: true }).start();

  const isIncome = item.amount > 0;
  const catConfig = CATEGORIES[item.category] || { icon: 'question-circle', color: theme.subtext };

  const renderLeftActions = () => (
    <View style={[styles.deleteAction, { backgroundColor: theme.delete }]}>
      <FontAwesome5 name="trash-alt" size={16} color="white" />
    </View>
  );

  return (
    <Swipeable
      ref={swipeableRef}
      friction={2}
      leftThreshold={40}
      renderLeftActions={renderLeftActions}
      overshootLeft={false}
      onSwipeableOpen={(direction) => {
        if (direction === 'left') {
          onDelete(item, swipeableRef);
        }
      }}
    >
      <Animated.View style={[{ transform: [{ scale: scaleAnim }] }]}>
        <Pressable 
          onPressIn={handlePressIn} 
          onPressOut={handlePressOut}
          style={[
            styles.row, 
            !isLast && { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: theme.border }
          ]}
        >
          <View style={[styles.categoryIcon, { backgroundColor: catConfig.color + '20' }]}>
            <FontAwesome5 name={catConfig.icon} size={15} color={catConfig.color} />
          </View>

          <View style={styles.textContainer}>
            <Text style={[styles.largeText, { color: theme.title }]} numberOfLines={1}>
              {item.title}
            </Text>
            <Text style={[styles.categorySubtitle, { color: theme.subtext }]}>{item.category}</Text>
          </View>

          <View style={styles.amountContainer}>
            <FontAwesome5 
              name={isIncome ? "arrow-up" : "arrow-down"} 
              size={12} 
              color={theme.subtext} 
              style={styles.arrowIcon}
            />
            <Text style={[styles.amountText, { color: theme.subtext }]}>
              LKR {Math.abs(item.amount).toFixed(2)}
            </Text>
          </View>
        </Pressable>
      </Animated.View>
    </Swipeable>
  );
};

// --- MAIN SCREEN ---
export default function Index() {
  const [activeTab, setActiveTab] = useState<'transactions' | 'cards'>('transactions');
  
  // Transaction State
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [selectedItem, setSelectedItem] = useState<Transaction | null>(null);
  const [activeSwipeable, setActiveSwipeable] = useState<React.RefObject<Swipeable> | null>(null);

  // Credit Cards State
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [cardModalVisible, setCardModalVisible] = useState(false);
  const [bankName, setBankName] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [cardHolder, setCardHolder] = useState('');
  const [expiry, setExpiry] = useState('');
  const [cardType, setCardType] = useState<'Visa' | 'Mastercard'>('Visa');

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  // Load Storage Data
  const loadData = async () => {
    try {
      const txData = await AsyncStorage.getItem('transactions');
      if (txData) setTransactions(JSON.parse(txData));

      const cardData = await AsyncStorage.getItem('user_cards');
      if (cardData) setCards(JSON.parse(cardData));
    } catch (e) {
      console.error("Load error", e);
    }
  };

  useFocusEffect(useCallback(() => { loadData(); }, []));

  // --- GROUPING TRANSACTIONS BY DATE ---
  const groupedTransactions = useMemo(() => {
    const groups: { [key: string]: GroupedTransactions } = {};

    transactions.forEach(tx => {
      const dateObj = new Date(tx.date);
      const dateKey = dateObj.toISOString().split('T')[0];

      if (!groups[dateKey]) {
        groups[dateKey] = {
          dateKey,
          formattedDate: dateObj.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
          dailyTotal: 0,
          items: []
        };
      }
      groups[dateKey].items.push(tx);
      groups[dateKey].dailyTotal += tx.amount;
    });

    return Object.values(groups).sort((a, b) => new Date(b.dateKey).getTime() - new Date(a.dateKey).getTime());
  }, [transactions]);

  // Transaction Delete
  const triggerDelete = (item: Transaction, ref: React.RefObject<Swipeable>) => {
    setSelectedItem(item);
    setActiveSwipeable(ref);
    setDeleteModalVisible(true);
  };

  const cancelDelete = () => {
    if (activeSwipeable?.current) {
      activeSwipeable.current.close();
    }
    setDeleteModalVisible(false);
    setSelectedItem(null);
    setActiveSwipeable(null);
  };

  const deleteItem = async () => {
    if (!selectedItem) return;
    const updated = transactions.filter(t => t.id !== selectedItem.id);
    setTransactions(updated);
    await AsyncStorage.setItem('transactions', JSON.stringify(updated));
    setDeleteModalVisible(false);
    setActiveSwipeable(null);
  };

  // Card Logic
  const formatCardNumber = (num: string) => {
    return num.replace(/\s?/g, '').replace(/(\d{4})/g, '$1 ').trim();
  };

  const handleCardNumberChange = (text: string) => {
    setCardNumber(text);
    const cleanNum = text.replace(/\s/g, '');
    if (cleanNum.startsWith('4')) {
      setCardType('Visa');
    } else if (cleanNum.startsWith('5')) {
      setCardType('Mastercard');
    }
  };

  const saveCard = async () => {
    if (!bankName || cardNumber.length < 4) {
      Alert.alert("Error", "Please fill in all details");
      return;
    }

    const newCard: CreditCard = {
      id: Date.now().toString(),
      bankName,
      cardNumber: cardNumber.replace(/\s/g, ''),
      expiry,
      type: cardType,
      cardHolder,
      color: theme.accent,
    };

    const updated = [...cards, newCard];
    setCards(updated);
    await AsyncStorage.setItem('user_cards', JSON.stringify(updated));
    setCardModalVisible(false);
    setBankName(''); setCardNumber(''); setExpiry(''); setCardHolder(''); setCardType('Visa');
  };

  const deleteCard = (id: string) => {
    Alert.alert("Delete Card", "Are you sure you want to remove this card?", [
      { text: "Cancel", style: "cancel" },
      { 
        text: "Delete", 
        style: "destructive", 
        onPress: async () => {
          const updated = cards.filter(c => c.id !== id);
          setCards(updated);
          await AsyncStorage.setItem('user_cards', JSON.stringify(updated));
        } 
      }
    ]);
  };

  const handleAddButtonPress = () => {
    if (activeTab === 'transactions') {
      router.push('/addTransaction');
    } else {
      setCardModalVisible(true);
    }
  };

  const balance = transactions.reduce((acc, curr) => acc + curr.amount, 0);

  const renderCard = ({ item }: { item: CreditCard }) => (
    <View style={[styles.creditCard, { backgroundColor: item.color }]}>
      <View style={styles.cardTop}>
        <Text style={styles.cardBank}>{item.bankName}</Text>
        <View style={styles.cardTopRight}>
          <FontAwesome5 
            name={item.type === 'Visa' ? 'cc-visa' : 'cc-mastercard'} 
            size={28} 
            color="white" 
            style={{ marginRight: 12 }} 
          />
          <Pressable onPress={() => deleteCard(item.id)}>
            <FontAwesome5 name='trash' size={16} color="rgba(255,255,255,0.8)" />
          </Pressable>
        </View>
      </View>
      
      <Text style={styles.cardNumberText}>{formatCardNumber(item.cardNumber)}</Text>
      
      <View style={styles.cardBottom}>
        <View>
          <Text style={styles.cardLabel}>CARD HOLDER</Text>
          <Text style={styles.cardHolder}>{item.cardHolder || '---'}</Text>
        </View>
        <View style={{ alignItems: 'flex-end' }}>
          <Text style={styles.cardLabel}>EXPIRY</Text>
          <Text style={styles.cardExpiry}>{item.expiry || 'MM/YY'}</Text>
        </View>
      </View>
    </View>
  );

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
        
        {/* --- DYNAMIC HEADER --- */}
        <View style={styles.header}>
          <View>
            <Text style={[styles.headerSubtitle, { color: theme.subtext }]}>Manage Your</Text>
            <Text style={[styles.headerTitle, { color: theme.title }]}>
              {activeTab === 'transactions' ? 'Transactions' : 'Credit Cards'}
            </Text>
          </View>
          
          {/* Dynamic Plus Action Button */}
          <Pressable style={[styles.addBtn, { backgroundColor: theme.accent }]} onPress={handleAddButtonPress}>
            <FontAwesome5 name="plus" size={18} color="white" />
          </Pressable>
        </View>

        {/* --- HORIZONTAL SEGMENT CONTROL BAR --- */}
        <View style={[styles.tabBarContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
          <Pressable 
            style={[styles.tabButton, activeTab === 'transactions' && { backgroundColor: theme.accent }]}
            onPress={() => setActiveTab('transactions')}
          >
            <Text style={[styles.tabText, { color: activeTab === 'transactions' ? '#FFFFFF' : theme.subtext }]}>
              Transactions
            </Text>
          </Pressable>
          <Pressable 
            style={[styles.tabButton, activeTab === 'cards' && { backgroundColor: theme.accent }]}
            onPress={() => setActiveTab('cards')}
          >
            <Text style={[styles.tabText, { color: activeTab === 'cards' ? '#FFFFFF' : theme.subtext }]}>
              Cards
            </Text>
          </Pressable>
        </View>

        {/* --- TAB 1: TRANSACTIONS VIEW --- */}
        {activeTab === 'transactions' ? (
          <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
            
            {/* Total Balance Card */}
            <View style={[styles.balanceCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
              <View style={styles.balanceHeader}>
                <View style={styles.balancePill}>
                  <FontAwesome5 name="wallet" size={12} color={theme.accent} style={{ marginRight: 6 }} />
                  <Text style={[styles.balanceLabel, { color: theme.subtext }]}>Net Balance</Text>
                </View>
                <FontAwesome5 name="chart-line" size={16} color={theme.accent} />
              </View>
              <Text style={[styles.balanceMainText, { color: theme.title }]}>
                LKR {balance.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </Text>
            </View>

            {/* Grouped Transactions List */}
            {groupedTransactions.length === 0 ? (
              <View style={styles.emptyContainer}>
                <FontAwesome5 name="wallet" size={48} color={theme.border} />
                <Text style={[styles.emptyText, { color: theme.subtext }]}>No transactions yet.</Text>
              </View>
            ) : (
              groupedTransactions.map((group) => (
                <View key={group.dateKey} style={[styles.groupCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
                  <View style={[styles.groupHeader, { borderBottomColor: theme.border }]}>
                    <Text style={[styles.groupDateText, { color: theme.subtext }]}>{group.formattedDate}</Text>
                    <Text style={[styles.groupTotalText, { color: theme.title }]}>
                      {group.dailyTotal >= 0 ? '+' : ''}LKR {group.dailyTotal.toFixed(2)}
                    </Text>
                  </View>

                  {group.items.map((item, index) => (
                    <TransactionRow
                      key={item.id}
                      item={item}
                      theme={theme}
                      onDelete={triggerDelete}
                      isLast={index === group.items.length - 1}
                    />
                  ))}
                </View>
              ))
            )}
          </ScrollView>
        ) : (
          /* --- TAB 2: CARDS VIEW --- */
          <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100, paddingTop: 10 }}>
            <FlatList
              scrollEnabled={false}
              data={cards}
              renderItem={renderCard}
              keyExtractor={item => item.id}
              ListEmptyComponent={
                <View style={styles.emptyContainer}>
                  <FontAwesome5 name="credit-card" size={48} color={theme.border} style={{ marginBottom: 10 }} />
                  <Text style={[styles.emptyText, { color: theme.subtext }]}>No cards added yet.</Text>
                </View>
              }
            />
          </ScrollView>
        )}

        {/* --- DELETE TRANSACTION MODAL --- */}
        <Modal transparent visible={deleteModalVisible} animationType="fade">
          <View style={styles.modalOverlay}>
            <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
              <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Transaction</Text>
              <Text style={[styles.modalSub, { color: theme.subtext }]}>
                Are you sure you want to remove "{selectedItem?.title}"?
              </Text>
              <View style={styles.modalActions}>
                <Pressable style={styles.modalBtn} onPress={cancelDelete}>
                  <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
                </Pressable>
                <Pressable style={[styles.modalBtn, { backgroundColor: theme.delete }]} onPress={deleteItem}>
                  <Text style={{ color: 'white', fontWeight: '600' }}>Delete</Text>
                </Pressable>
              </View>
            </View>
          </View>
        </Modal>

        {/* --- ADD CARD MODAL --- */}
        <Modal visible={cardModalVisible} animationType="slide" transparent>
          <View style={styles.cardModalOverlay}>
            <View style={[styles.cardModalContent, { backgroundColor: theme.card }]}>
              <View style={styles.modalHeader}>
                <Text style={[styles.modalTitle, { color: theme.title }]}>Add New Card</Text>
                <Pressable onPress={() => setCardModalVisible(false)}>
                  <FontAwesome5 name="times" size={20} color={theme.subtext} />
                </Pressable>
              </View>

              {/* Card Type Selector */}
              <View style={styles.typeSelectorContainer}>
                <Pressable 
                  style={[
                    styles.typeOption, 
                    { borderColor: theme.border },
                    cardType === 'Visa' && { backgroundColor: theme.accent, borderColor: theme.accent }
                  ]}
                  onPress={() => setCardType('Visa')}
                >
                  <FontAwesome5 name="cc-visa" size={20} color={cardType === 'Visa' ? 'white' : theme.subtext} />
                  <Text style={[styles.typeText, { color: cardType === 'Visa' ? 'white' : theme.subtext }]}>Visa</Text>
                </Pressable>

                <Pressable 
                  style={[
                    styles.typeOption, 
                    { borderColor: theme.border },
                    cardType === 'Mastercard' && { backgroundColor: theme.accent, borderColor: theme.accent }
                  ]}
                  onPress={() => setCardType('Mastercard')}
                >
                  <FontAwesome5 name="cc-mastercard" size={20} color={cardType === 'Mastercard' ? 'white' : theme.subtext} />
                  <Text style={[styles.typeText, { color: cardType === 'Mastercard' ? 'white' : theme.subtext }]}>Mastercard</Text>
                </Pressable>
              </View>

              <TextInput
                placeholder="Bank Name"
                placeholderTextColor={theme.subtext + '80'}
                style={[styles.input, { backgroundColor: theme.background, color: theme.title, borderColor: theme.border }]}
                value={bankName}
                onChangeText={setBankName}
              />
              <TextInput
                placeholder="Card Holder"
                placeholderTextColor={theme.subtext + '80'}
                style={[styles.input, { backgroundColor: theme.background, color: theme.title, borderColor: theme.border }]}
                value={cardHolder}
                onChangeText={setCardHolder}
              />
              <TextInput
                placeholder="Card Number"
                keyboardType="number-pad"
                maxLength={16}
                placeholderTextColor={theme.subtext + '80'}
                style={[styles.input, { backgroundColor: theme.background, color: theme.title, borderColor: theme.border }]}
                value={cardNumber}
                onChangeText={handleCardNumberChange}
              />
              <TextInput
                placeholder="Expiry (MM/YY)"
                placeholderTextColor={theme.subtext + '80'}
                style={[styles.input, { backgroundColor: theme.background, color: theme.title, borderColor: theme.border }]}
                value={expiry}
                onChangeText={setExpiry}
              />

              <Pressable style={[styles.saveBtn, { backgroundColor: theme.accent }]} onPress={saveCard}>
                <Text style={styles.saveBtnText}>Save Card</Text>
              </Pressable>
            </View>
          </View>
        </Modal>

        <Footer />
      </SafeAreaView>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 15, paddingBottom: 10 },
  headerSubtitle: { fontSize: 13, fontWeight: '600', textTransform: 'uppercase', letterSpacing: 0.5 },
  headerTitle: { fontSize: 28, fontWeight: '800', letterSpacing: -0.5, marginTop: 2 },
  addBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  
  // Tab Bar Styles
  tabBarContainer: { 
    flexDirection: 'row', 
    marginHorizontal: 20, 
    marginVertical: 10, 
    borderRadius: 16, 
    padding: 4, 
    borderWidth: 1 
  },
  tabButton: { 
    flex: 1, 
    paddingVertical: 10, 
    borderRadius: 12, 
    alignItems: 'center', 
    justifyContent: 'center' 
  },
  tabText: { fontSize: 14, fontWeight: '700' },

  // Balance Card Styles
  balanceCard: { 
    padding: 20, 
    borderRadius: 24, 
    marginBottom: 16, 
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 2
  },
  balanceHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  balancePill: { flexDirection: 'row', alignItems: 'center' },
  balanceLabel: { fontSize: 13, fontWeight: '700', textTransform: 'uppercase', letterSpacing: 0.5 },
  balanceMainText: { fontSize: 25, fontWeight: '800', letterSpacing: -1 },

  // Grouped Card Styles
  scroll: { flex: 1, paddingHorizontal: 20, paddingTop: 6 },
  groupCard: { borderRadius: 20, borderWidth: 1, marginBottom: 16, overflow: 'hidden' },
  groupHeader: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    paddingHorizontal: 16, 
    paddingVertical: 12, 
    borderBottomWidth: StyleSheet.hairlineWidth 
  },
  groupDateText: { fontSize: 13, fontWeight: '700' },
  groupTotalText: { fontSize: 13, fontWeight: '800' },

  // Transaction Row Styles
  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, paddingHorizontal: 16 },
  categoryIcon: { width: 38, height: 38, borderRadius: 12, justifyContent: 'center', alignItems: 'center', marginRight: 14 },
  textContainer: { flex: 1, paddingRight: 8 },
  largeText: { fontSize: 15, fontWeight: '700' },
  categorySubtitle: { fontSize: 12, fontWeight: '500', marginTop: 2 },
  amountContainer: { flexDirection: 'row', alignItems: 'center' },
  arrowIcon: { marginRight: 6 },
  amountText: { fontSize: 14, fontWeight: '800' },
  deleteAction: { justifyContent: 'center', alignItems: 'center', width: 60, height: '100%' },

  // Credit Card Component Styles
  creditCard: { width: '100%', borderRadius: 24, padding: 22, elevation: 5, marginBottom: 16 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 25 },
  cardTopRight: { flexDirection: 'row', alignItems: 'center' },
  cardBank: { color: 'white', fontSize: 18, fontWeight: '800', letterSpacing: 0.5 },
  cardNumberText: { color: 'white', fontSize: 20, fontWeight: '700', letterSpacing: 3, marginBottom: 25 },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  cardLabel: { color: 'rgba(255,255,255,0.6)', fontSize: 9, fontWeight: '700', marginBottom: 4 },
  cardHolder: { color: 'white', fontSize: 13, fontWeight: '600', letterSpacing: 1 },
  cardExpiry: { color: 'white', fontSize: 13, fontWeight: '600' },

  emptyContainer: { alignItems: 'center', marginTop: 80 },
  emptyText: { marginTop: 12, fontSize: 15, fontWeight: '500' },

  // Modals
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  modalContent: { width: '85%', padding: 24, borderRadius: 28, alignItems: 'center' },
  modalTitle: { fontSize: 18, fontWeight: '800', marginBottom: 8 },
  modalSub: { textAlign: 'center', marginBottom: 20, lineHeight: 20, fontSize: 14 },
  modalActions: { flexDirection: 'row', gap: 12 },
  modalBtn: { flex: 1, padding: 14, borderRadius: 14, alignItems: 'center' },

  cardModalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end' },
  cardModalContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: 25, minHeight: 480 },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  
  // Card Type Selector Styles
  typeSelectorContainer: { flexDirection: 'row', gap: 12, marginBottom: 15 },
  typeOption: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', paddingVertical: 12, borderRadius: 14, borderWidth: 1, gap: 8 },
  typeText: { fontWeight: '700', fontSize: 14 },

  input: { padding: 16, borderRadius: 18, borderWidth: 1, marginBottom: 15, fontSize: 15 },
  saveBtn: { padding: 18, borderRadius: 18, alignItems: 'center', marginTop: 10 },
  saveBtnText: { color: 'white', fontSize: 16, fontWeight: '700' },
});