package main

import (
	"context"
	"net/http"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"gorm.io/gorm"
)

// L4: JWT secret
const jwtSecret = "change-me-in-production"

type contextKey string

const (
	ctxUserID   contextKey = "userID"
	ctxLogEntry contextKey = "logEntry"
)

// logEntry is a mutable pointer shared between queryLogger (outer) and jwtAuth (inner).
type logEntry struct {
	userID *uint
}

// --- JWT helpers ---

type jwtClaims struct {
	jwt.RegisteredClaims
	UserID uint `json:"uid"`
}

func makeToken(userID uint) (string, error) {
	c := jwtClaims{
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
		UserID: userID,
	}
	return jwt.NewWithClaims(jwt.SigningMethodHS256, c).SignedString([]byte(jwtSecret))
}

// --- Middleware ---

// L4: jwtAuth validates the Bearer token and injects userID into context.
// It also updates the logEntry pointer so queryLogger can record the user.
func jwtAuth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		auth := r.Header.Get("Authorization")
		if !strings.HasPrefix(auth, "Bearer ") {
			writeErr(w, http.StatusUnauthorized, "missing or invalid Authorization header")
			return
		}
		tok, err := jwt.ParseWithClaims(auth[7:], &jwtClaims{}, func(t *jwt.Token) (any, error) {
			if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, jwt.ErrSignatureInvalid
			}
			return []byte(jwtSecret), nil
		})
		if err != nil || !tok.Valid {
			writeErr(w, http.StatusUnauthorized, "invalid token")
			return
		}
		c := tok.Claims.(*jwtClaims)
		uid := c.UserID

		// Propagate user ID to the outer queryLogger via the shared pointer
		if entry, ok := r.Context().Value(ctxLogEntry).(*logEntry); ok {
			entry.userID = &uid
		}

		ctx := context.WithValue(r.Context(), ctxUserID, uid)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// L4: queryLogger records method, path, status code, latency, and user ID for every request.
func queryLogger(db *gorm.DB) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			sw := &statusWriter{ResponseWriter: w, status: http.StatusOK}
			entry := &logEntry{}
			ctx := context.WithValue(r.Context(), ctxLogEntry, entry)

			next.ServeHTTP(sw, r.WithContext(ctx))

			// Save asynchronously so logging never delays the response
			go db.Create(&QueryLog{
				UserID:   entry.userID,
				Method:   r.Method,
				Path:     r.URL.Path,
				Duration: time.Since(start).Milliseconds(),
				Status:   sw.status,
			})
		})
	}
}

// statusWriter wraps http.ResponseWriter to capture the status code written by handlers.
type statusWriter struct {
	http.ResponseWriter
	status int
	wrote  bool
}

func (sw *statusWriter) WriteHeader(code int) {
	if !sw.wrote {
		sw.status = code
		sw.wrote = true
		sw.ResponseWriter.WriteHeader(code)
	}
}

func (sw *statusWriter) Write(b []byte) (int, error) {
	if !sw.wrote {
		sw.status = http.StatusOK
		sw.wrote = true
	}
	return sw.ResponseWriter.Write(b)
}
