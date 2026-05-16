package main

import (
	"time"

	"gorm.io/gorm"
)

// L1: Users table
type User struct {
	gorm.Model
	Username     string `gorm:"uniqueIndex;not null" json:"username"`
	Email        string `gorm:"uniqueIndex;not null" json:"email"`
	PasswordHash string `gorm:"not null"             json:"-"`
}

// L1: Friendships
type Friendship struct {
	UserID    uint      `gorm:"primaryKey" json:"user_id"`
	FriendID  uint      `gorm:"primaryKey" json:"friend_id"`
	CreatedAt time.Time `                  json:"created_at"`
}

// L1: Posts table
type Post struct {
	gorm.Model
	UserID  uint   `gorm:"index;not null"    json:"user_id"`
	User    User   `gorm:"foreignKey:UserID" json:"user,omitempty"`
	Content string `gorm:"not null"          json:"content"`
}

// L1: Comments table (1:N)
type Comment struct {
	gorm.Model
	PostID  uint   `gorm:"index;not null"    json:"post_id"`
	UserID  uint   `gorm:"index;not null"    json:"user_id"`
	User    User   `gorm:"foreignKey:UserID" json:"user,omitempty"`
	Content string `gorm:"not null"          json:"content"`
}

// L1: Likes
type Like struct {
	PostID    uint      `gorm:"primaryKey" json:"post_id"`
	UserID    uint      `gorm:"primaryKey" json:"user_id"`
	CreatedAt time.Time `                  json:"created_at"`
}

// L4: QueryLog
type QueryLog struct {
	gorm.Model
	UserID   *uint  `gorm:"index" json:"user_id,omitempty"`
	Method   string `             json:"method"`
	Path     string `gorm:"index" json:"path"`
	Duration int64  `             json:"duration_ms"`
	Status   int    `             json:"status"`
}
