package com.example.doan

import android.content.Context
import android.content.SharedPreferences
import com.example.doan.Models.JwtResponse
import com.example.doan.Utils.SessionManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * FIX Medium #11: Unit tests cho SessionManager
 */
class SessionManagerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var sessionManager: SessionManager
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        
        sessionManager = SessionManager(mockContext)
    }
    
    @Test
    fun `isLoggedIn returns false when not logged in`() {
        `when`(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(false)
        
        assertFalse(sessionManager.isLoggedIn())
    }
    
    @Test
    fun `isLoggedIn returns true when logged in`() {
        `when`(mockSharedPreferences.getBoolean("is_logged_in", false)).thenReturn(true)
        
        assertTrue(sessionManager.isLoggedIn())
    }
    
    @Test
    fun `getToken returns null when no token`() {
        `when`(mockSharedPreferences.getString("jwt_token", null)).thenReturn(null)
        
        assertNull(sessionManager.getToken())
    }
    
    @Test
    fun `getToken returns token when exists`() {
        val expectedToken = "test_token_123"
        `when`(mockSharedPreferences.getString("jwt_token", null)).thenReturn(expectedToken)
        
        assertEquals(expectedToken, sessionManager.getToken())
    }
    
    @Test
    fun `getUserId returns -1 when not set`() {
        `when`(mockSharedPreferences.getInt("user_id", -1)).thenReturn(-1)
        
        assertEquals(-1, sessionManager.getUserId())
    }
    
    @Test
    fun `getUserId returns correct id when set`() {
        `when`(mockSharedPreferences.getInt("user_id", -1)).thenReturn(42)
        
        assertEquals(42, sessionManager.getUserId())
    }
    
    @Test
    fun `getRole returns USER by default`() {
        `when`(mockSharedPreferences.getString("role", "USER")).thenReturn("USER")
        
        assertEquals("USER", sessionManager.getRole())
    }
    
    @Test
    fun `isManager returns true for MANAGER role`() {
        `when`(mockSharedPreferences.getString("role", "USER")).thenReturn("MANAGER")
        
        assertTrue(sessionManager.isManager())
    }
    
    @Test
    fun `isManager returns false for USER role`() {
        `when`(mockSharedPreferences.getString("role", "USER")).thenReturn("USER")
        
        assertFalse(sessionManager.isManager())
    }
    
    @Test
    fun `saveLoginSession saves all fields`() {
        sessionManager.saveLoginSession(
            userId = 1,
            username = "testuser",
            email = "test@example.com",
            fullName = "Test User",
            phone = "0123456789",
            role = "USER",
            memberTier = "BRONZE",
            token = "access_token",
            refreshToken = "refresh_token",
            avatar = "avatar_url"
        )
        
        verify(mockEditor).putBoolean("is_logged_in", true)
        verify(mockEditor).putInt("user_id", 1)
        verify(mockEditor).putString("username", "testuser")
        verify(mockEditor).putString("email", "test@example.com")
        verify(mockEditor).putString("full_name", "Test User")
        verify(mockEditor).putString("phone", "0123456789")
        verify(mockEditor).putString("role", "USER")
        verify(mockEditor).putString("member_tier", "BRONZE")
        verify(mockEditor).putString("jwt_token", "access_token")
        verify(mockEditor).putString("refresh_token", "refresh_token")
        verify(mockEditor).putString("avatar", "avatar_url")
        verify(mockEditor).apply()
    }
    
    @Test
    fun `logout clears session`() {
        sessionManager.logout()
        
        verify(mockEditor).clear()
        verify(mockEditor).apply()
    }
    
    // FIX Medium #11: Thêm edge case tests
    @Test
    fun `getFullName returns null when not set`() {
        `when`(mockSharedPreferences.getString("full_name", null)).thenReturn(null)
        
        assertNull(sessionManager.getFullName())
    }
    
    @Test
    fun `getFullName returns correct name when set`() {
        val expectedName = "Test User"
        `when`(mockSharedPreferences.getString("full_name", null)).thenReturn(expectedName)
        
        assertEquals(expectedName, sessionManager.getFullName())
    }
    
    @Test
    fun `getRefreshToken returns null when not set`() {
        `when`(mockSharedPreferences.getString("refresh_token", null)).thenReturn(null)
        
        assertNull(sessionManager.getRefreshToken())
    }
    
    @Test
    fun `getRefreshToken returns token when exists`() {
        val expectedToken = "refresh_token_123"
        `when`(mockSharedPreferences.getString("refresh_token", null)).thenReturn(expectedToken)
        
        assertEquals(expectedToken, sessionManager.getRefreshToken())
    }
    
    @Test
    fun `saveLoginSession with null values handles gracefully`() {
        // Should not throw exception when called with null values
        sessionManager.saveLoginSession(
            userId = 1,
            username = "testuser",
            email = null,
            fullName = null,
            phone = null,
            role = "USER",
            memberTier = null,
            token = "access_token",
            refreshToken = null,
            avatar = null
        )
        
        verify(mockEditor).putBoolean("is_logged_in", true)
        verify(mockEditor).putInt("user_id", 1)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `isAdmin returns true for ADMIN role`() {
        `when`(mockSharedPreferences.getString("role", "USER")).thenReturn("ADMIN")
        
        assertTrue(sessionManager.isAdmin())
    }
    
    @Test
    fun `isAdmin returns false for USER role`() {
        `when`(mockSharedPreferences.getString("role", "USER")).thenReturn("USER")
        
        assertFalse(sessionManager.isAdmin())
    }
}
