package main

import (
	"log/slog"

	"github.com/glebarez/sqlite"
	"gorm.io/gorm"
	gormlog "gorm.io/gorm/logger"
)

func initDB(path string) *gorm.DB {
	db, err := gorm.Open(sqlite.Open(path), &gorm.Config{
		Logger: gormlog.Default.LogMode(gormlog.Silent),
	})
	if err != nil {
		panic(err)
	}

	// L1: enable WAL mode and foreign key constraints
	db.Exec("PRAGMA journal_mode=WAL")
	db.Exec("PRAGMA foreign_keys=ON")

	// L1: create all tables with correct FK relationships
	if err := db.AutoMigrate(
		&User{},
		&Friendship{},
		&Post{},
		&Comment{},
		&Like{},
		&QueryLog{},
	); err != nil {
		panic(err)
	}

	// L3: extra indexes for fast search and time-range filtering
	db.Exec("CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at)")
	db.Exec("CREATE INDEX IF NOT EXISTS idx_query_logs_created_at ON query_logs(created_at)")

	slog.Info("database ready", "path", path)
	return db
}
