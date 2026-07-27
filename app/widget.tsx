'use no memo';
import React from 'react';
import { FlexWidget, ListWidget, TextWidget } from 'react-native-android-widget';

interface WidgetProp {
    task: any[];
    date: string;
}

export function MyWidget({ task, date }: WidgetProp) {
    const now = new Date();

    return (
        <ListWidget
            style={{
                height: 'match_parent',
                width: 'match_parent',
                backgroundColor: '#000', // Clean white background
            }}
        >
            {/* Centered Header with Bottom Border */}
            <FlexWidget
                style={{
                    backgroundColor: '#000', 
                    borderBottomWidth: 1,
                    borderBottomColor: '#fff',
                    padding: 12,
                    width: 'match_parent'
                }}
                clickAction='OPEN_APP'
            >
                <TextWidget 
                    style={{ 
                        fontSize: 16, 
                        fontWeight: 'bold', 
                        color: '#fff',
                        width: 'match_parent',
                        textAlign: 'center' // Centering the title
                    }} 
                    text={date.toUpperCase()} 
                />
            </FlexWidget>

            {task.map((item, index) => {
                const isOverdue = new Date(item.date) < now;
                
                return (
                        <FlexWidget
                            key={index}
                            style={{
                                width: 'match_parent',
                                backgroundColor: '#000', // Always white boxes
                                padding: 12,
                            }}
                            clickAction='OPEN_APP'
                        >
                            <FlexWidget style={{ flexDirection: 'row', alignItems: 'center' }}>
                                {/* COLORED CIRCLE: Red for late, Blue for rest */}
                                <FlexWidget
                                    style={{
                                        width: 12,
                                        height: 12,
                                        borderRadius: 6,
                                        backgroundColor: isOverdue ? '#fe8080' : '#6b82f7',
                                        marginRight: 10,
                                    }}
                                />
                                <TextWidget 
                                    style={{ 
                                        fontSize: 15, 
                                        fontWeight: 'bold', 
                                        color: '#fff' 
                                    }} 
                                    text={item.title} 
                                />
                            </FlexWidget>
                        </FlexWidget>
                );
            })}
        </ListWidget>
    );
}