package main

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/go-chi/chi/v5"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

// --- Shared helpers ---

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(v)
}

func writeErr(w http.ResponseWriter, code int, msg string) {
	writeJSON(w, code, map[string]string{"error": msg})
}

func readJSON(r *http.Request, v any) error {
	return json.NewDecoder(r.Body).Decode(v)
}

func urlID(r *http.Request, param string) (uint, error) {
	n, err := strconv.ParseUint(chi.URLParam(r, param), 10, 64)
	return uint(n), err
}

func currentUser(r *http.Request) uint {
	uid, _ := r.Context().Value(ctxUserID).(uint)
	return uid
}

// --- Auth (L4: bcrypt + JWT) ---

func register(db *gorm.DB) http.HandlerFunc {
	type req struct {
		Username string `json:"username"`
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	return func(w http.ResponseWriter, r *http.Request) {
		var body req
		if err := readJSON(r, &body); err != nil || body.Username == "" || body.Email == "" || body.Password == "" {
			writeErr(w, http.StatusBadRequest, "username, email and password required")
			return
		}
		hash, err := bcrypt.GenerateFromPassword([]byte(body.Password), bcrypt.DefaultCost)
		if err != nil {
			writeErr(w, http.StatusInternalServerError, "internal error")
			return
		}
		user := User{Username: body.Username, Email: body.Email, PasswordHash: string(hash)}
		if err := db.Create(&user).Error; err != nil {
			writeErr(w, http.StatusConflict, "username or email already taken")
			return
		}
		tok, _ := makeToken(user.ID)
		writeJSON(w, http.StatusCreated, map[string]any{"token": tok, "user_id": user.ID})
	}
}

func login(db *gorm.DB) http.HandlerFunc {
	type req struct {
		Email    string `json:"email"`
		Password string `json:"password"`
	}
	return func(w http.ResponseWriter, r *http.Request) {
		var body req
		if err := readJSON(r, &body); err != nil {
			writeErr(w, http.StatusBadRequest, "invalid json")
			return
		}
		// L4: parameterised query — no SQL injection possible via GORM
		var user User
		if err := db.Where("email = ?", body.Email).First(&user).Error; err != nil {
			writeErr(w, http.StatusUnauthorized, "invalid credentials")
			return
		}
		if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(body.Password)) != nil {
			writeErr(w, http.StatusUnauthorized, "invalid credentials")
			return
		}
		tok, _ := makeToken(user.ID)
		writeJSON(w, http.StatusOK, map[string]any{"token": tok, "user_id": user.ID})
	}
}

// --- Posts (L2: CRUD + L3: search & filter) ---

func listPosts(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()

		page, _ := strconv.Atoi(q.Get("page"))
		if page < 1 {
			page = 1
		}
		limit, _ := strconv.Atoi(q.Get("limit"))
		if limit < 1 || limit > 100 {
			limit = 20
		}

		tx := db.Model(&Post{}).Preload("User")

		// L3: full-text search via LIKE (uses idx_posts_content index)
		if search := q.Get("q"); search != "" {
			tx = tx.Where("content LIKE ?", "%"+search+"%")
		}
		// L3: filter by author
		if authorID := q.Get("author_id"); authorID != "" {
			tx = tx.Where("user_id = ?", authorID)
		}
		// L3: date range filter (uses idx_posts_created_at index)
		if from := q.Get("from"); from != "" {
			if t, err := time.Parse("2006-01-02", from); err == nil {
				tx = tx.Where("created_at >= ?", t)
			}
		}
		if to := q.Get("to"); to != "" {
			if t, err := time.Parse("2006-01-02", to); err == nil {
				tx = tx.Where("created_at <= ?", t.Add(24*time.Hour-time.Second))
			}
		}

		var total int64
		tx.Count(&total)

		var posts []Post
		tx.Order("created_at desc").Offset((page - 1) * limit).Limit(limit).Find(&posts)

		writeJSON(w, http.StatusOK, map[string]any{
			"posts": posts,
			"total": total,
			"page":  page,
			"limit": limit,
		})
	}
}

func createPost(db *gorm.DB) http.HandlerFunc {
	type req struct {
		Content string `json:"content"`
	}
	return func(w http.ResponseWriter, r *http.Request) {
		var body req
		if err := readJSON(r, &body); err != nil || body.Content == "" {
			writeErr(w, http.StatusBadRequest, "content required")
			return
		}
		post := Post{UserID: currentUser(r), Content: body.Content}
		db.Create(&post)
		db.Preload("User").First(&post, post.ID)
		writeJSON(w, http.StatusCreated, post)
	}
}

func getPost(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		var post Post
		if err := db.Preload("User").First(&post, id).Error; errors.Is(err, gorm.ErrRecordNotFound) {
			writeErr(w, http.StatusNotFound, "post not found")
			return
		}
		writeJSON(w, http.StatusOK, post)
	}
}

func updatePost(db *gorm.DB) http.HandlerFunc {
	type req struct {
		Content string `json:"content"`
	}
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		var post Post
		if err := db.First(&post, id).Error; errors.Is(err, gorm.ErrRecordNotFound) {
			writeErr(w, http.StatusNotFound, "post not found")
			return
		}
		if post.UserID != currentUser(r) {
			writeErr(w, http.StatusForbidden, "not your post")
			return
		}
		var body req
		if err := readJSON(r, &body); err != nil || body.Content == "" {
			writeErr(w, http.StatusBadRequest, "content required")
			return
		}
		db.Model(&post).Update("content", body.Content)
		writeJSON(w, http.StatusOK, post)
	}
}

func deletePost(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		var post Post
		if err := db.First(&post, id).Error; errors.Is(err, gorm.ErrRecordNotFound) {
			writeErr(w, http.StatusNotFound, "post not found")
			return
		}
		if post.UserID != currentUser(r) {
			writeErr(w, http.StatusForbidden, "not your post")
			return
		}
		db.Delete(&post)
		w.WriteHeader(http.StatusNoContent)
	}
}

// --- Comments (L2) ---

func listComments(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		postID, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		var comments []Comment
		db.Where("post_id = ?", postID).Preload("User").Order("created_at asc").Find(&comments)
		writeJSON(w, http.StatusOK, comments)
	}
}

func createComment(db *gorm.DB) http.HandlerFunc {
	type req struct {
		Content string `json:"content"`
	}
	return func(w http.ResponseWriter, r *http.Request) {
		postID, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		var body req
		if err := readJSON(r, &body); err != nil || body.Content == "" {
			writeErr(w, http.StatusBadRequest, "content required")
			return
		}
		if err := db.First(&Post{}, postID).Error; errors.Is(err, gorm.ErrRecordNotFound) {
			writeErr(w, http.StatusNotFound, "post not found")
			return
		}
		comment := Comment{PostID: postID, UserID: currentUser(r), Content: body.Content}
		db.Create(&comment)
		db.Preload("User").First(&comment, comment.ID)
		writeJSON(w, http.StatusCreated, comment)
	}
}

// --- Likes ---

func likePost(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		postID, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		like := Like{PostID: postID, UserID: currentUser(r), CreatedAt: time.Now()}
		if err := db.Create(&like).Error; err != nil {
			writeErr(w, http.StatusConflict, "already liked")
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}

func unlikePost(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		postID, err := urlID(r, "id")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		db.Where("post_id = ? AND user_id = ?", postID, currentUser(r)).Delete(&Like{})
		w.WriteHeader(http.StatusNoContent)
	}
}

// --- Friends ---

func listFriends(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		uid := currentUser(r)
		var friendships []Friendship
		db.Where("user_id = ?", uid).Find(&friendships)

		ids := make([]uint, 0, len(friendships))
		for _, f := range friendships {
			ids = append(ids, f.FriendID)
		}
		var friends []User
		if len(ids) > 0 {
			db.Find(&friends, ids)
		}
		writeJSON(w, http.StatusOK, friends)
	}
}

func addFriend(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		uid := currentUser(r)
		fid, err := urlID(r, "fid")
		if err != nil || fid == uid {
			writeErr(w, http.StatusBadRequest, "invalid friend id")
			return
		}
		if err := db.First(&User{}, fid).Error; errors.Is(err, gorm.ErrRecordNotFound) {
			writeErr(w, http.StatusNotFound, "user not found")
			return
		}
		f := Friendship{UserID: uid, FriendID: fid, CreatedAt: time.Now()}
		if err := db.Create(&f).Error; err != nil {
			writeErr(w, http.StatusConflict, "already friends")
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}

func removeFriend(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		uid := currentUser(r)
		fid, err := urlID(r, "fid")
		if err != nil {
			writeErr(w, http.StatusBadRequest, "invalid id")
			return
		}
		db.Where("user_id = ? AND friend_id = ?", uid, fid).Delete(&Friendship{})
		w.WriteHeader(http.StatusNoContent)
	}
}

// --- Admin (L4: monitoring) ---

func adminLogs(db *gorm.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		page, _ := strconv.Atoi(q.Get("page"))
		if page < 1 {
			page = 1
		}
		limit, _ := strconv.Atoi(q.Get("limit"))
		if limit < 1 || limit > 200 {
			limit = 50
		}

		var total int64
		db.Model(&QueryLog{}).Count(&total)

		var logs []QueryLog
		db.Order("created_at desc").Offset((page - 1) * limit).Limit(limit).Find(&logs)

		writeJSON(w, http.StatusOK, map[string]any{
			"logs":  logs,
			"total": total,
			"page":  page,
			"limit": limit,
		})
	}
}
