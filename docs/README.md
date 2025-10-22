# Documentation Index

Welcome to the Spring Boot ACL Demo documentation! This index will help you find the right documentation for your needs.

## 🚀 Getting Started

New to the project? Start here:

1. **[Main README](../README.md)** - Project overview and quick start
2. **[Developer Guide](../DEVELOPER_GUIDE.md)** - Comprehensive development guide
3. **[Architecture Diagrams](ARCHITECTURE_DIAGRAMS.md)** - Visual system architecture

## 📚 Core Documentation

### For Developers

| Document | Purpose | Audience |
|----------|---------|----------|
| **[Developer Guide](../DEVELOPER_GUIDE.md)** | Complete guide covering architecture, ACL concepts, implementation, and extension | All developers |
| [ACL Setup Guide](ACL_SETUP.md) | ACL infrastructure configuration and setup details | Backend developers |
| [Domain Model](DOMAIN_MODEL.md) | Entity relationships and database schema | Backend developers |
| [Architecture Diagrams](ARCHITECTURE_DIAGRAMS.md) | Visual diagrams of flows and components | All developers |

### For API Users

| Document | Purpose | Audience |
|----------|---------|----------|
| [API Examples](API_EXAMPLES.md) | Complete REST API examples with curl commands | API consumers |
| [Permission API](PERMISSION_API.md) | Permission management endpoints reference | API consumers |
| [OpenAPI Specification](OPENAPI_SPEC.yaml) | Swagger/OpenAPI 3.0 API spec | API consumers, Tools |

### For Testing

| Document | Purpose | Audience |
|----------|---------|----------|
| [Testing Guide](TESTING_GUIDE.md) | Comprehensive testing strategies and examples | QA, Developers |
| [Troubleshooting](TROUBLESHOOTING.md) | Common issues and solutions | All users |

## 🎯 Quick Navigation by Task

### I want to...

**Understand how ACL works**
→ Read: [Developer Guide - ACL Concepts](../DEVELOPER_GUIDE.md#acl-concepts)

**Set up the project**
→ Read: [Main README - Quick Start](../README.md#quick-start)

**Use the REST API**
→ Read: [API Examples](API_EXAMPLES.md)

**Grant permissions to users**
→ Read: [Permission API](PERMISSION_API.md)

**Add a new domain entity with ACL**
→ Read: [Developer Guide - Extending the System](../DEVELOPER_GUIDE.md#extending-the-system)

**Write tests for ACL functionality**
→ Read: [Testing Guide](TESTING_GUIDE.md)

**Debug permission issues**
→ Read: [Troubleshooting](TROUBLESHOOTING.md)

**Understand permission inheritance**
→ Read: [Architecture Diagrams - Permission Inheritance](ARCHITECTURE_DIAGRAMS.md#permission-inheritance-hierarchy)

**Improve performance**
→ Read: [Developer Guide - Performance Optimization](../DEVELOPER_GUIDE.md#advanced-topics)

**View API in Swagger UI**
→ Import: [OpenAPI Specification](OPENAPI_SPEC.yaml) into Swagger Editor

## 📖 Documentation by Complexity

### Beginner Level

Start with these if you're new to ACL or the project:

1. [Main README](../README.md) - Project overview
2. [API Examples](API_EXAMPLES.md) - Simple API usage examples
3. [Architecture Diagrams](ARCHITECTURE_DIAGRAMS.md) - Visual overview

### Intermediate Level

Once you understand the basics:

1. [Developer Guide](../DEVELOPER_GUIDE.md) - In-depth development guide
2. [ACL Setup Guide](ACL_SETUP.md) - Infrastructure details
3. [Testing Guide](TESTING_GUIDE.md) - Testing strategies
4. [Permission API](PERMISSION_API.md) - Advanced permission management

### Advanced Level

For extending and optimizing the system:

1. [Developer Guide - Advanced Topics](../DEVELOPER_GUIDE.md#advanced-topics)
2. [Developer Guide - Extending the System](../DEVELOPER_GUIDE.md#extending-the-system)
3. [Domain Model](DOMAIN_MODEL.md) - Data model deep dive
4. [Troubleshooting](TROUBLESHOOTING.md) - Complex issues

## 🔍 Search by Keyword

### ACL Concepts
- Object-Level Security → [Developer Guide - ACL Concepts](../DEVELOPER_GUIDE.md#acl-concepts)
- Permission Inheritance → [Architecture Diagrams](ARCHITECTURE_DIAGRAMS.md#permission-inheritance-hierarchy)
- Security Identity (SID) → [Developer Guide - ACL Concepts](../DEVELOPER_GUIDE.md#acl-concepts)
- Object Identity (OID) → [Developer Guide - ACL Concepts](../DEVELOPER_GUIDE.md#acl-concepts)

### Implementation
- Controllers → [API Examples](API_EXAMPLES.md)
- Services → [Developer Guide - Implementation Details](../DEVELOPER_GUIDE.md#implementation-details)
- Security Annotations → [Developer Guide - Implementation Details](../DEVELOPER_GUIDE.md#implementation-details)
- Custom Permissions → [Developer Guide - Extending](../DEVELOPER_GUIDE.md#adding-custom-permissions)

### Database
- Schema → [ACL Setup Guide](ACL_SETUP.md) + [Domain Model](DOMAIN_MODEL.md)
- Tables → [Architecture Diagrams - Database Schema](ARCHITECTURE_DIAGRAMS.md#acl-database-schema)
- Queries → [Troubleshooting](TROUBLESHOOTING.md#database-issues)

### Performance
- Caching → [Architecture Diagrams - Caching](ARCHITECTURE_DIAGRAMS.md#caching-architecture)
- Optimization → [Developer Guide - Advanced Topics](../DEVELOPER_GUIDE.md#advanced-topics)
- Batch Operations → [Permission API](PERMISSION_API.md)

### Security
- Authentication → [Troubleshooting - Auth Issues](TROUBLESHOOTING.md#authentication--authorization-issues)
- Authorization → [Developer Guide - ACL Concepts](../DEVELOPER_GUIDE.md#acl-concepts)
- Access Control → [API Examples](API_EXAMPLES.md)

## 📦 Document Summaries

### [Developer Guide](../DEVELOPER_GUIDE.md) (~1250 lines)
**The main comprehensive guide** covering:
- System architecture and component breakdown
- Complete ACL concepts explanation
- Implementation details with code examples
- API reference and examples
- Extension guidelines
- Testing strategies
- Troubleshooting tips
- Advanced topics

### [OpenAPI Specification](OPENAPI_SPEC.yaml) (~1100 lines)
**Complete API specification** including:
- All REST endpoints with parameters
- Request/response schemas
- Authentication requirements
- Error responses
- Example requests

### [Architecture Diagrams](ARCHITECTURE_DIAGRAMS.md) (~655 lines)
**Visual documentation** with Mermaid diagrams:
- System architecture overview
- ACL permission check flow
- Permission inheritance hierarchy
- Database schema
- Caching architecture
- Audit trail flow
- Request processing flows

### [Testing Guide](TESTING_GUIDE.md) (~909 lines)
**Comprehensive testing documentation**:
- Test structure and organization
- Unit testing strategies
- Integration testing approaches
- ACL-specific test scenarios
- Testing best practices
- Common test patterns

### [Troubleshooting Guide](TROUBLESHOOTING.md) (~909 lines)
**Problem-solving reference**:
- Quick diagnostics
- Common issues by category
- Step-by-step solutions
- SQL queries for debugging
- Performance troubleshooting
- Configuration issues

### [API Examples](API_EXAMPLES.md) (~481 lines)
**Practical API usage examples**:
- CRUD operations for all resources
- Permission management examples
- Access control scenarios
- Error handling examples
- Complete curl commands

### [Permission API](PERMISSION_API.md) (~472 lines)
**Permission management reference**:
- Grant/revoke endpoints
- Bulk operations
- Permission discovery
- Inheritance checking
- Use case examples

### [ACL Setup Guide](ACL_SETUP.md) (~228 lines)
**Infrastructure setup guide**:
- ACL component overview
- Configuration details
- Database schema explanation
- Testing the setup
- Extension examples

### [Domain Model](DOMAIN_MODEL.md) (~237 lines)
**Data model documentation**:
- Entity descriptions
- Relationships
- ACL design patterns
- Seed data explanation

## 🆘 Need Help?

### Can't find what you're looking for?

1. **Search**: Use Ctrl+F / Cmd+F in your browser
2. **Index**: Check this document's table of contents
3. **Examples**: Look at [API Examples](API_EXAMPLES.md)
4. **Troubleshoot**: Check [Troubleshooting Guide](TROUBLESHOOTING.md)

### Common Questions

**Q: How do I start the application?**  
A: See [Main README - Quick Start](../README.md#quick-start)

**Q: How do I grant permissions?**  
A: See [Permission API - Grant Permissions](PERMISSION_API.md#1-grant-permissions)

**Q: Why am I getting 403 Forbidden?**  
A: See [Troubleshooting - 403 Issues](TROUBLESHOOTING.md#issue-403-forbidden)

**Q: How does inheritance work?**  
A: See [Architecture Diagrams - Inheritance](ARCHITECTURE_DIAGRAMS.md#permission-inheritance-hierarchy)

**Q: How do I add custom permissions?**  
A: See [Developer Guide - Custom Permissions](../DEVELOPER_GUIDE.md#adding-custom-permissions)

## 📊 Documentation Coverage

This documentation covers:

- ✅ Architecture & Design
- ✅ Setup & Configuration
- ✅ API Usage & Examples
- ✅ Testing Strategies
- ✅ Troubleshooting
- ✅ Extension Guidelines
- ✅ Performance Optimization
- ✅ Security Best Practices
- ✅ Visual Diagrams
- ✅ OpenAPI Specification

## 🔄 Documentation Updates

This documentation is version-controlled alongside the code. When making changes:

1. Update relevant documentation files
2. Update this index if adding new documents
3. Ensure examples match current code
4. Test all code snippets
5. Update version numbers

---

**Last Updated:** January 2024  
**Documentation Version:** 1.0.0

---

## Contributing to Documentation

Found an issue or want to improve the docs?

1. Check [Contributing Guidelines](../README.md#contributing)
2. Follow existing documentation style
3. Include examples where appropriate
4. Test all code snippets
5. Update this index if needed

---

**Happy coding! 🚀**
