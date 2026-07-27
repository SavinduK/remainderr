import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import React, { useState } from 'react';
import { Alert, Pressable, ScrollView, StatusBar, StyleSheet, Text, TextInput, View, useColorScheme } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

const CATEGORIES = [
  { label: 'Food', icon: 'utensils' },
  { label: 'Travel', icon: 'car' },
  { label: 'Shopping', icon: 'shopping-cart' },
  { label: 'Health', icon: 'heartbeat' },
  { label: 'Other', icon: 'ellipsis-h' },
];

export default function AddTransaction() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  const [type, setType] = useState<'income' | 'expense'>('expense');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Food');
  const [description, setDescription] = useState('');

  const handleSave = async () => {
    if (!amount || isNaN(parseFloat(amount))) {
      Alert.alert("Invalid Amount", "Please enter a valid numeric value.");
      return;
    }
    if (!description.trim()) {
      Alert.alert("Missing Detail", "Please provide a description.");
      return;
    }

    const newEntry = {
      id: Date.now().toString(),
      title: description,
      amount: type === 'expense' ? -Math.abs(parseFloat(amount)) : Math.abs(parseFloat(amount)),
      category,
      date: new Date().toISOString(),
    };

    try {
      const existingData = await AsyncStorage.getItem('transactions');
      const transactions = existingData ? JSON.parse(existingData) : [];
      transactions.unshift(newEntry);
      await AsyncStorage.setItem('transactions', JSON.stringify(transactions));
      router.replace('/transactions');
    } catch (e) {
      Alert.alert("Error", "Failed to save data.");
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.backBtn}>
          <FontAwesome5 name="arrow-left" size={18} color={theme.title} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: theme.title }]}>New Transaction</Text>
        <View style={{ width: 40 }} />
      </View>

      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 40 }}>
        {/* Toggle Switch */}
        <View style={[styles.typeContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
          <Pressable 
            onPress={() => setType('income')}
            style={[styles.typeBtn, type === 'income' && { backgroundColor: theme.success }]}
          >
            <Text style={[styles.typeText, { color: type === 'income' ? 'white' : theme.subtext }]}>Income</Text>
          </Pressable>
          <Pressable 
            onPress={() => setType('expense')}
            style={[styles.typeBtn, type === 'expense' && { backgroundColor: theme.delete }]}
          >
            <Text style={[styles.typeText, { color: type === 'expense' ? 'white' : theme.subtext }]}>Expense</Text>
          </Pressable>
        </View>

        {/* Amount Input */}
        <Text style={[styles.label, { color: theme.accent }]}>Amount (LKR)</Text>
        <TextInput
          style={[styles.amountInput, { color: theme.title, borderBottomColor: theme.border }]}
          value={amount}
          onChangeText={setAmount}
          placeholder="0.00"
          placeholderTextColor={theme.border}
          keyboardType="numeric"
        />

        {/* Description Input */}
        <Text style={[styles.label, { color: theme.accent, marginTop: 30 }]}>Description</Text>
        <TextInput
          style={[styles.input, { backgroundColor: theme.card, color: theme.title, borderColor: theme.border }]}
          value={description}
          onChangeText={setDescription}
          placeholder="What was this for?"
          placeholderTextColor={theme.subtext}
        />

        {/* Category Selection */}
        <Text style={[styles.label, { color: theme.accent, marginTop: 30 }]}>Category</Text>
        <View style={styles.categoryGrid}>
          {CATEGORIES.map((item) => (
            <Pressable
              key={item.label}
              onPress={() => setCategory(item.label)}
              style={[
                styles.categoryCard,
                { backgroundColor: theme.card, borderColor: category === item.label ? theme.accent : theme.border }
              ]}
            >
              <FontAwesome5 
                name={item.icon} 
                size={18} 
                color={category === item.label ? theme.accent : theme.icon} 
              />
              <Text style={[styles.categoryLabel, { color: category === item.label ? theme.title : theme.subtext }]}>
                {item.label}
              </Text>
            </Pressable>
          ))}
        </View>

        <Pressable style={[styles.saveBtn, { backgroundColor: theme.accent }]} onPress={handleSave}>
          <Text style={styles.saveBtnText}>Save Transaction</Text>
        </Pressable>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20 },
  headerTitle: { fontSize: 22, fontWeight: '800', letterSpacing: -0.5 },
  backBtn: { width: 40, height: 40, justifyContent: 'center' },
  scroll: { flex: 1, paddingHorizontal: 25 },
  typeContainer: { flexDirection: 'row', padding: 6, borderRadius: 18, borderWidth: 1, marginBottom: 40 },
  typeBtn: { flex: 1, paddingVertical: 12, borderRadius: 14, alignItems: 'center' },
  typeText: { fontWeight: '700', fontSize: 14 },
  label: { fontSize: 12, fontWeight: '800', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 10 },
  amountInput: { fontSize: 20, fontWeight: '800', paddingVertical: 10, borderBottomWidth: 2 },
  input: { padding: 18, borderRadius: 20, fontSize: 16, borderWidth: 1, fontWeight: '600' },
  categoryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  categoryCard: { width: '30%', paddingVertical: 20, borderRadius: 20, alignItems: 'center', borderWidth: 2, marginBottom: 5 },
  categoryLabel: { fontSize: 12, fontWeight: '700', marginTop: 8 },
  saveBtn: { marginTop: 40, padding: 20, borderRadius: 22, alignItems: 'center', shadowOpacity: 0.2, shadowRadius: 10, elevation: 5 },
  saveBtnText: { color: 'white', fontSize: 16, fontWeight: '800' },
});